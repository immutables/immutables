/*
    Copyright 2013-2014 Immutables.org authors

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
package org.immutables.common.repository.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedBytes;
import com.mongodb.DBCallback;
import com.mongodb.DBCollection;
import com.mongodb.DBDecoder;
import com.mongodb.DBDecoderFactory;
import com.mongodb.DBEncoder;
import com.mongodb.DBObject;
import com.mongodb.DefaultDBEncoder;
import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonGenerator;
import de.undercouch.bson4jackson.BsonParser;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.bson.BSONCallback;
import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.OutputBuffer;
import org.immutables.common.marshal.Marshaler;

/**
 * MongoDB driver specific encoding and jumping hoops.
 */
@SuppressWarnings("resource")
public final class BsonEncoding {
  private static final BsonFactory BSON_FACTORY = new BsonFactory().enable(BsonParser.Feature.HONOR_DOCUMENT_LENGTH);

  /**
   * This field name will cause an mongodb confuse if not unwraped correctly so it's may be a good
   * choice.
   */
  private static final String PREENCODED_VALUE_WRAPPER_FIELD_NAME = "$";

  private BsonEncoding() {}

  /**
   * Althought it may seem that reparsing is bizzare, but it is one [of not so many] ways to do
   * proper marshaling. This kind of inneficiency will only hit query constraints that have many
   * object with custom marshaling, which considered to be a rare case.
   * @param marshalableValue the value in a marshalable wrapper
   * @return object converted to MongoDB driver's {@link BSONObject}.
   */
  public static Object unwrapBsonable(RepositorySupport.MarshalableWrapper marshalableValue) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      BsonGenerator generator = BSON_FACTORY.createGenerator(outputStream);
      generator.writeStartObject();
      generator.writeFieldName(PREENCODED_VALUE_WRAPPER_FIELD_NAME);
      marshalableValue.marshalWrapped(generator);
      generator.writeEndObject();
      generator.close();

      BasicBSONDecoder bsonDecoder = new BasicBSONDecoder();
      BSONObject object = bsonDecoder.readObject(outputStream.toByteArray());
      return object.get(PREENCODED_VALUE_WRAPPER_FIELD_NAME);
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }
  }

  public static <T> T unmarshalDbObject(DBObject dbObject, Marshaler<T> marshaler) throws IOException {
    BasicOutputBuffer buffer = new BasicOutputBuffer();
    encoder().writeObject(buffer, dbObject);
    JsonParser parser = BSON_FACTORY.createParser(buffer.toByteArray());
    parser.nextToken();
    T instance = marshaler.unmarshalInstance(parser);
    parser.close();
    return instance;
  }

  private static class CountingOutputBufferStream extends OutputStream {
    final OutputBuffer buffer;
    int count;

    public CountingOutputBufferStream(OutputBuffer buffer) {
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
          return ((WritableObjectPosition) object).writeCurrent(buffer);
        }
        return DefaultDBEncoder.FACTORY.create().writeObject(buffer, object);
      } catch (IOException ex) {
        throw Throwables.propagate(ex);
      }
    }
  }

  public static <T> DBObject wrapUpdateObject(T instance, Marshaler<T> marshaler) {
    return new UpdateObject<>(instance, marshaler);
  }

  public static <T> List<DBObject> wrapInsertObjectList(ImmutableList<T> list, Marshaler<T> marshaler) {
    return new InsertObjectList<>(list, marshaler);
  }

  interface WritableObjectPosition {
    int writeCurrent(OutputBuffer buffer) throws IOException;
  }

  private static class UpdateObject<T> implements DBObject, WritableObjectPosition {

    private final T instance;
    private final Marshaler<T> marshaler;

    UpdateObject(T instance, Marshaler<T> marshaler) {
      this.instance = instance;
      this.marshaler = marshaler;
    }

    public int writeCurrent(OutputBuffer buffer) throws IOException {
      CountingOutputBufferStream outputStream = new CountingOutputBufferStream(buffer);
      JsonGenerator generator = BSON_FACTORY.createGenerator(outputStream);
      marshaler.marshalInstance(generator, instance);
      generator.close();
      return outputStream.count;
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
      return ImmutableSet.of();
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
    private JsonGenerator generator;

    private CountingOutputBufferStream outputStream;
    private final Marshaler<T> marshaler;

    InsertObjectList(ImmutableList<T> list, Marshaler<T> marshaler) {
      this.list = list;
      this.marshaler = marshaler;
    }

    public int writeCurrent(OutputBuffer buffer) throws IOException {
      createGeneratorIfNecessary(buffer);
      int previousByteCount = outputStream.count;
      marshaler.marshalInstance(generator, list.get(position));
      if (isLastPosition()) {
        closeGenerator();
      }
      return outputStream.count - previousByteCount;
    }

    private void closeGenerator() throws IOException {
      if (generator != null) {
        generator.close();
        generator = null;
      }
    }

    private void createGeneratorIfNecessary(OutputBuffer buffer) throws IOException {
      if (generator == null) {
        outputStream = new CountingOutputBufferStream(buffer);
        generator = BSON_FACTORY.createGenerator(outputStream);
      }
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
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public Iterator<DBObject> iterator() {
      return Iterators.emptyIterator();
    }

    @Override
    public int lastIndexOf(Object o) {
      throw new UnsupportedOperationException();
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

  public static <T> ImmutableList<T> unwrapResultObjectList(List<DBObject> result) {
    if (result.isEmpty()) {
      return ImmutableList.of();
    }
    // Safe as long as caller will use same T for decoder and unwrap
    @SuppressWarnings("unchecked")
    List<T> results = ((ResultDecoder<T>) result.get(0)).results;

    return ImmutableList.copyOf(results);
  }

  public static <T> DBDecoderFactory newResultDecoderFor(Marshaler<T> marshaler, int expectedSize) {
    return new ResultDecoder<>(marshaler, expectedSize);
  }

  /**
   * Special input stream that operates from as writable byte buffer that is filled with BSON object
   * from other input stream ({@link #resetObjectFrom(InputStream)}).
   * Extending buffered input stream
   * to prevent excessive wraping in another buffered stream by {@link BsonParser}
   */
  static final class ObjectBufferInputStream extends BufferedInputStream {
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

  private static class ResultDecoder<T> implements DBDecoderFactory, DBDecoder, DBObject {
    final List<T> results;
    private final Marshaler<T> marshaler;
    @Nullable
    private BsonParser parser;

    private ObjectBufferInputStream bufferStream = new ObjectBufferInputStream(2012);

    public ResultDecoder(Marshaler<T> marshaler, int expectedSize) {
      this.marshaler = marshaler;
      this.results = Lists.newArrayListWithExpectedSize(expectedSize);
    }

    public BsonParser createParserIfNecessary() throws IOException {
      if (parser != null) {
        parser.close();
      }
      parser = BSON_FACTORY.createParser(bufferStream);
      return parser;
    }

    @Override
    public DBObject decode(InputStream inputStream, DBCollection collection) throws IOException {
      bufferStream.resetObjectFrom(inputStream);
      createParserIfNecessary();
      parser.nextToken();
      T unmarshaledObject = marshaler.unmarshalInstance(parser);
      results.add(unmarshaledObject);
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
