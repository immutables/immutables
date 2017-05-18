/*
   Copyright 2013-2015 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.mongo.repository.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedBytes;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonWriter;
import com.mongodb.*;
import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonGenerator;
import de.undercouch.bson4jackson.BsonParser;
import org.bson.*;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.OutputBuffer;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;

/**
 * MongoDB driver specific encoding and jumping hoops.
 */
@SuppressWarnings("resource")
public final class BsonEncoding {
  private static final BsonFactory BSON_FACTORY = new BsonFactory()
      .enable(BsonParser.Feature.HONOR_DOCUMENT_LENGTH);

  private static final JsonFactory JSON_FACTORY = new JsonFactory()
      .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
      .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

  /**
   * This field name will cause an MongoDB confuse if not unwrapped correctly so it may be a good
   * choice.
   */
  private static final String PREENCODED_VALUE_WRAPPER_FIELD_NAME = "$";

  private BsonEncoding() {}

  /**
   * Although it may seem that re-parsing is bizarre, but it is one [of not so many] ways to do
   * proper marshaling. This kind of inefficiency will only hit query constraints that have many
   * object with custom marshaling, which considered to be a rare case.
   * @param adapted adapted value that know how to write itself to {@link JsonWriter}
   * @return object converted to MongoDB driver's {@link BSONObject}.
   */
  public static Object unwrapBsonable(Support.Adapted<?> adapted) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      BsonGenerator generator = BSON_FACTORY.createGenerator(outputStream);
      BsonWriter writer = new BsonWriter(generator);
      writer.beginObject().name(PREENCODED_VALUE_WRAPPER_FIELD_NAME);
      adapted.write(writer);
      writer.endObject();
      writer.close();
      BSONObject object = new BasicBSONDecoder().readObject(outputStream.toByteArray());
      return object.get(PREENCODED_VALUE_WRAPPER_FIELD_NAME);
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }
  }

  public static DBObject unwrapJsonable(String json) {
    try {
      JsonParser parser = JSON_FACTORY.createParser(json);
      parser.nextToken();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      BsonGenerator generator = BSON_FACTORY.createGenerator(outputStream);
      generator.copyCurrentStructure(parser);
      generator.close();
      parser.close();
      byte[] data = outputStream.toByteArray();
      return (DBObject) new LazyDBCallback(null).createObject(data, 0);
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }
  }

  public static <T> T unmarshalDbObject(DBObject dbObject, TypeAdapter<T> adaper) throws IOException {
    BasicOutputBuffer buffer = new BasicOutputBuffer();
    encoder().writeObject(buffer, dbObject);
    BsonParser parser = BSON_FACTORY.createParser(buffer.toByteArray());
    BsonReader reader = new BsonReader(parser);
    T instance = adaper.read(reader);
    reader.close();
    return instance;
  }

  private static class CountingOutputBufferStream extends OutputStream {
    final OutputBuffer buffer;
    int count;

    CountingOutputBufferStream(OutputBuffer buffer) {
      this.buffer = buffer;
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
      buffer.write(bytes, offset, length);
      count += length;
    }

    @Override
    public void write(int byteValue) throws IOException {
      buffer.write(byteValue);
      count++;
    }
  }

  public static DBEncoder encoder() {
    return Encoder.ENCODER;
  }

  enum Encoder implements DBEncoder {
    ENCODER;

    @Override
    public int writeObject(OutputBuffer buffer, BSONObject object) {
      try {
        if (object instanceof WritableObjectPosition) {
          return ((WritableObjectPosition) object).writePlainCurrent(buffer);
        }
        return DefaultDBEncoder.FACTORY.create().writeObject(buffer, object);
      } catch (IOException ex) {
        throw Throwables.propagate(ex);
      }
    }
  }

  public static <T> DBObject wrapUpdateObject(T instance, TypeAdapter<T> adaper) {
    return new UpdateObject<>(instance, adaper);
  }

  public static <T> List<DBObject> wrapInsertObjectList(ImmutableList<T> list, TypeAdapter<T> adaper) {
    return new InsertObjectList<>(list, adaper);
  }

  interface WritableObjectPosition {
    int writeCurrent(OutputBuffer buffer) throws IOException;

    int writePlainCurrent(OutputBuffer buffer) throws IOException;
  }

  private static DBObject cloneCurrentPosition(WritableObjectPosition position) {
    OutputBuffer buffer = new BasicOutputBuffer();
    try {
      position.writePlainCurrent(buffer);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't serialize current instance", e);
    }

    final DBObject bson = new LazyWriteableDBObject(buffer.toByteArray(), new LazyBSONCallback());
    final BasicDBObject copy = new BasicDBObject();
    copy.putAll(bson);
    return copy;
  }

  private static class UpdateObject<T> implements DBObject, WritableObjectPosition {

    private final T instance;
    private final TypeAdapter<T> adaper;

    private DBObject cached;

    UpdateObject(T instance, TypeAdapter<T> adaper) {
      this.instance = instance;
      this.adaper = adaper;
    }

    @Override
    public int writeCurrent(OutputBuffer buffer) throws IOException {
      CountingOutputBufferStream outputStream = new CountingOutputBufferStream(buffer);
      BsonWriter writer = new BsonWriter(BSON_FACTORY.createGenerator(outputStream));
      adaper.write(writer, instance);
      writer.close();
      return outputStream.count;
    }

    @Override
    public int writePlainCurrent(OutputBuffer buffer) throws IOException {
      return writeCurrent(buffer);
    }

    private DBObject cached() {
      if (cached != null) return cached;
      cached = cloneCurrentPosition(this);
      return cached;
    }

    @Override
    public Object put(String key, Object v) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(BSONObject o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map m) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object get(String key) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map toMap() {
      return cached().toMap();
    }

    @Override
    public Object removeField(String key) {
      throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean containsKey(String s) {
      return cached().containsKey(s);
    }

    @Override
    public boolean containsField(String s) {
      return cached().containsField(s);
    }

    @Override
    public Set<String> keySet() {
      return cached().keySet();
    }

    @Override
    public void markAsPartialObject() {}

    @Override
    public boolean isPartialObject() {
      return false;
    }
  }

  private static class InsertObjectList<T> implements DBObject, List<DBObject>, WritableObjectPosition {
    private final ImmutableList<T> list;
    private int position;
    @Nullable
    private JsonWriter writer;

    private DBObject cached;
    private int cachedIndex;

    private CountingOutputBufferStream outputStream;
    private final TypeAdapter<T> adaper;

    InsertObjectList(ImmutableList<T> list, TypeAdapter<T> adaper) {
      this.list = list;
      this.adaper = adaper;
      this.cachedIndex = -1;
    }

    @Override
    public int writeCurrent(OutputBuffer buffer) throws IOException {
      createGeneratorIfNecessary(buffer);
      int previousByteCount = outputStream.count;
      adaper.write(writer, list.get(position));
      if (isLastPosition()) {
        closeWriter();
      }
      return outputStream.count - previousByteCount;
    }

    @Override
    public int writePlainCurrent(OutputBuffer buffer) throws IOException {
      CountingOutputBufferStream outputStream = new CountingOutputBufferStream(buffer);
      BsonWriter writer = new BsonWriter(BSON_FACTORY.createGenerator(outputStream));
      adaper.write(writer, list.get(position));
      writer.close();
      return outputStream.count;
    }

    private void closeWriter() throws IOException {
      if (writer != null) {
        writer.close();
        writer = null;
      }
    }

    private void createGeneratorIfNecessary(OutputBuffer buffer) throws IOException {
      if (writer == null) {
        outputStream = new CountingOutputBufferStream(buffer);
        writer = new BsonWriter(BSON_FACTORY.createGenerator(outputStream));
      }
    }

    private DBObject cached() {
      if (cachedIndex == position) {
        return cached;
      }

      cached = cloneCurrentPosition(this);
      cachedIndex = position;
      return cached;
    }


    private boolean isLastPosition() {
      return position == list.size() - 1;
    }

    @Override
    public DBObject get(int index) {
      position = index;
      return this;
    }

    @Override
    public int size() {
      return list.size();
    }

    @Override
    public Iterator<DBObject> iterator() {
      return new Iterator<DBObject>() {
        int i = 0;
        @Override
        public boolean hasNext() {
          return i < list.size();
        }
        @Override
        public DBObject next() {
          if (!hasNext()) {
            throw new NoSuchElementException("At index: " + i);
          }

          return get(i++);
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public Object put(String key, Object v) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(BSONObject o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map m) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object get(String key) {
      return cached().get(key);
    }

    @Override
    public Map toMap() {
      return cached().toMap();
    }


    @Override
    public Object removeField(String key) {
      throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean containsKey(String s) {
      return cached().containsKey(s);
    }

    @Override
    public boolean containsField(String s) {
      return cached().containsField(s);
    }

    @Override
    public Set<String> keySet() {
     return cached().keySet();
    }

    @Override
    public void markAsPartialObject() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPartialObject() {
      return false;
    }

    @Override
    public boolean add(DBObject e) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, DBObject element) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends DBObject> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends DBObject> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
      return list.indexOf(o);
    }

    @Override
    public boolean isEmpty() {
      return list.isEmpty();
    }

    @Override
    public int lastIndexOf(Object o) {
      return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<DBObject> listIterator() {
      return ImmutableList.<DBObject>of().listIterator();
    }

    @Override
    public ListIterator<DBObject> listIterator(int index) {
      return ImmutableList.<DBObject>of().listIterator();
    }

    @Override
    public DBObject set(int index, DBObject element) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DBObject remove(int index) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<DBObject> subList(int fromIndex, int toIndex) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <V> V[] toArray(V[] a) {
      throw new UnsupportedOperationException();
    }
  }

  public static <T> ImmutableList<T> unwrapResultObjectList(List<DBObject> result, TypeAdapter<T> adapter) {
    if (result.isEmpty()) {
      return ImmutableList.of();
    }

    // Fongo ignores any decoders
    if (result.get(0) instanceof BasicDBObject) {
      try {
        return convertDBObject(result, adapter);
      } catch (IOException e) {
        throw new RuntimeException("Failed to convert DBObject", e);
      }
    }

    // Safe as long as caller will use same T for decoder and unwrap
    @SuppressWarnings("unchecked") List<T> results = ((ResultDecoder<T>) result.get(0)).results;
    return ImmutableList.copyOf(results);
  }

  private static <T> ImmutableList<T> convertDBObject(List<DBObject> result, TypeAdapter<T> adapter) throws IOException {
    final List<T> list = Lists.newArrayListWithExpectedSize(result.size());
    final BSONEncoder encoder = new BasicBSONEncoder();
    for (DBObject obj: result ) {
      BsonReader parser = new BsonReader(BSON_FACTORY.createParser(encoder.encode(obj)));
      list.add(adapter.read(parser));
    }

    return ImmutableList.copyOf(list);
  }

  public static <T> DBDecoderFactory newResultDecoderFor(TypeAdapter<T> adaper, int expectedSize) {
    return new ResultDecoder<>(adaper, expectedSize);
  }

  /**
   * Special input stream that operates from as writable byte buffer that is filled with BSON object
   * from other input stream ({@link #resetObjectFrom(InputStream)}).
   * Extending buffered input stream
   * to prevent excessive wraping in another buffered stream by {@link BsonParser}
   */
  private static final class ObjectBufferInputStream extends BufferedInputStream {
    private byte[] buffer;
    private int position;
    private int limit;

    ObjectBufferInputStream(int capacity) {
      super(null, 1);
      ensureBufferWithCapacity(capacity);
    }

    private void ensureBufferWithCapacity(int capacity) {
      if (buffer == null || buffer.length < capacity) {
        Preconditions.checkArgument(capacity >= 4);
        byte[] temp = buffer;
        this.buffer = new byte[capacity];
        if (temp != null) {
          System.arraycopy(temp, 0, buffer, 0, temp.length);
        }
      }
    }

    void resetObjectFrom(InputStream inputStream) throws IOException {
      ByteStreams.readFully(inputStream, buffer, 0, Ints.BYTES);

      int objectSize = Ints.fromBytes(
          buffer[3],
          buffer[2],
          buffer[1],
          buffer[0]);

      ensureBufferWithCapacity(objectSize);

      ByteStreams.readFully(inputStream, buffer, Ints.BYTES, objectSize - Ints.BYTES);

      position = 0;
      limit = objectSize;
    }

    @Override
    public int available() throws IOException {
      return limit - position;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      len = Math.min(len, available());
      System.arraycopy(buffer, position, b, off, len);
      position += len;
      return len;
    }

    @Override
    public int read() throws IOException {
      if (available() > 0) {
        return UnsignedBytes.toInt(buffer[position++]);
      }
      return -1;
    }
  }

  private static final class ResultDecoder<T> implements DBDecoderFactory, DBDecoder, DBObject {
    final List<T> results;
    private final TypeAdapter<T> adaper;
    @Nullable
    private BsonReader parser;

    private final ObjectBufferInputStream bufferStream = new ObjectBufferInputStream(2012);

    ResultDecoder(TypeAdapter<T> adaper, int expectedSize) {
      this.adaper = adaper;
      this.results = Lists.newArrayListWithExpectedSize(expectedSize);
    }

    private BsonReader createParserIfNecessary() throws IOException {
      if (parser != null) {
        parser.close();
      }
      parser = new BsonReader(BSON_FACTORY.createParser(bufferStream));
      return parser;
    }

    @Override
    public DBObject decode(InputStream inputStream, DBCollection collection) throws IOException {
      bufferStream.resetObjectFrom(inputStream);
      createParserIfNecessary();
      T object = adaper.read(parser);
      results.add(object);
      return this;
    }

    @Override
    public DBDecoder create() {
      return this;
    }

    @Override
    public BSONObject readObject(byte[] b) {
      throw new UnsupportedOperationException();
    }

    @Override
    public BSONObject readObject(InputStream in) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public int decode(byte[] b, BSONCallback callback) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int decode(InputStream in, BSONCallback callback) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public DBCallback getDBCallback(DBCollection collection) {
      throw new UnsupportedOperationException();
    }

    @Override
    public DBObject decode(byte[] b, DBCollection collection) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object put(String key, Object v) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(BSONObject o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map m) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object get(String key) {
      return null;
    }

    @Override
    public Map toMap() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object removeField(String key) {
      throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public boolean containsKey(String s) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsField(String s) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void markAsPartialObject() {}

    @Override
    public boolean isPartialObject() {
      return false;
    }
  }
}
