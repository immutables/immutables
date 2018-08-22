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
package org.immutables.mongo.bson4gson;

import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.bson.AbstractBsonReader;
import org.bson.AbstractBsonReader.State;
import org.bson.BsonType;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.Reader;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapter of {@link JsonReader GSON Reader} reading directly from
 * <a href="http://bsonspec.org/">BSON encoded</a> documents.
 * It delegates most of the calls to {@link org.bson.BsonReader} which understands binary JSON
 * representation (default wire protocol
 * between mongo server and client). This class allows to instantiate immutable objects directly
 * from binary stream (eg. {@code byte[]}) bypassing intermediate
 * object instantiation (typically {@code byte[] -> DBObject-> Immutable} ). Internally generated
 * GSON {@link com.google.gson.TypeAdapter} will
 * read incoming bytes as stream API.
 * @see BsonWriter
 * @see <a href="http://mongodb.github.io/mongo-java-driver/3.5/bson/">Mongo Driver (BSON)</a>
 * @see <a href="http://bsonspec.org/">BSON spec</a>
 */
@NotThreadSafe
public class BsonReader extends JsonReader implements Wrapper<org.bson.BsonReader> {

  private static final Reader UNREADABLE_READER = new Reader() {
    @Override
    public int read(char[] buffer, int offset, int count) throws IOException {
      throw new AssertionError();
    }

    @Override
    public void close() throws IOException {
      throw new AssertionError();
    }
  };

  private final AbstractBsonReader delegate;

  /**
   * In order to be able to get current location of the BSON "cursor" (document or array start/end)
   * enforcing clients to
   * provide {@link AbstractBsonReader} which exposes this information via {@code getState()}
   * method, instead of more generic interface {@link BsonReader}.
   * @param delegate
   * @see AbstractBsonReader#getState()
   */
  BsonReader(org.bson.AbstractBsonReader delegate) {
    super(UNREADABLE_READER);
    this.delegate = checkNotNull(delegate, "delegate");
  }

  @Override
  public org.bson.BsonReader unwrap() {
    return this.delegate;
  }

  private void advance() {
    delegate.readBsonType();
  }

  @Override
  public void beginArray() throws IOException {
    delegate.readStartArray();
  }

  @Override
  public void endArray() throws IOException {
    delegate.readEndArray();
  }

  @Override
  public void beginObject() throws IOException {
    delegate.readStartDocument();
  }

  @Override
  public void endObject() throws IOException {
    delegate.readEndDocument();
  }

  @Override
  public boolean hasNext() throws IOException {
    if (!hasMoreElements()) {
      return false;
    }
    advance();
    return hasMoreElements();
  }

  private boolean hasMoreElements() {
    switch (state()) {
    case END_OF_DOCUMENT:
    case END_OF_ARRAY:
    case DONE:
      return false;
    default:
      return true;
    }
  }

  @Override
  public JsonToken peek() throws IOException {
    switch (state()) {
    case INITIAL:
    case SCOPE_DOCUMENT:
    case TYPE:
      advance();
      return toGsonToken(delegate.getCurrentBsonType());
    case NAME:
      return JsonToken.NAME;
    case END_OF_DOCUMENT:
      return JsonToken.END_OBJECT;
    case END_OF_ARRAY:
      return JsonToken.END_ARRAY;
    case DONE:
      return JsonToken.END_DOCUMENT;
    case VALUE:
      return toGsonToken(delegate.getCurrentBsonType());
    default:
      throw new IllegalStateException("Unexpected state: " + state() + " currentType:" +
          delegate.getCurrentBsonType());
    }
  }

  private State state() {
    return delegate.getState();
  }

  private static JsonToken toGsonToken(BsonType type) {
    switch (type) {
    case END_OF_DOCUMENT:
      return JsonToken.END_DOCUMENT;
    case DOCUMENT:
      return JsonToken.BEGIN_OBJECT;
    case ARRAY:
      return JsonToken.BEGIN_ARRAY;
    case BOOLEAN:
      return JsonToken.BOOLEAN;
    case STRING:
    case SYMBOL:
    case OBJECT_ID:
    case BINARY:
    case REGULAR_EXPRESSION:
      return JsonToken.STRING;
    case DATE_TIME:
    case DOUBLE:
    case INT32:
    case INT64:
    case TIMESTAMP:
    case DECIMAL128:
      return JsonToken.NUMBER;
    case NULL:
      return JsonToken.NULL;
    default:
      // not really sure what to do with this type
      return JsonToken.NULL;
    }
  }

  @Override
  public String nextName() throws IOException {
    return delegate.readName();
  }

  @Override
  public String nextString() throws IOException {
    return scalarToString();
  }

  /**
   * Gson library reads numbers lazily when using generic
   * {@link com.google.gson.internal.bind.TypeAdapters#JSON_ELEMENT} type adapter.
   * Number is read as string and then wrapped inside {@link LazilyParsedNumber}. This inefficiency
   * should only occur if reading numbers with generic JSON element API
   * and not using generated type adapters.
   * @see LazilyParsedNumber
   * @see com.google.gson.internal.bind.TypeAdapters#JSON_ELEMENT
   */
  private String scalarToString() {
    switch (delegate.getCurrentBsonType()) {
    case STRING:
      return delegate.readString();
    case SYMBOL:
      return delegate.readSymbol();
    case INT32:
      return Integer.toString(delegate.readInt32());
    case INT64:
      return Long.toString(delegate.readInt64());
    case DOUBLE:
      return Double.toString(delegate.readDouble());
    case DECIMAL128:
      return delegate.readDecimal128().toString();
    default:
      throw new IllegalStateException(
          "Unknown scalar type to be converted to string: " + delegate.getCurrentBsonType());
    }
  }

  @Override
  public boolean nextBoolean() throws IOException {
    return delegate.readBoolean();
  }

  @Override
  public void nextNull() throws IOException {
    delegate.readNull();
  }

  @Override
  public double nextDouble() throws IOException {
    switch (delegate.getCurrentBsonType()) {
    case INT32:
      return delegate.readInt32();
    case INT64:
      return delegate.readInt64();
    case DECIMAL128:
      return delegate.readDecimal128().bigDecimalValue().doubleValue();
    default:
      return delegate.readDouble();
    }
  }

  @Override
  public long nextLong() throws IOException {
    switch (delegate.getCurrentBsonType()) {
    case DOUBLE:
      return (long) delegate.readDouble();
    case INT32:
      return delegate.readInt32();
    case DECIMAL128:
      return delegate.readDecimal128().bigDecimalValue().longValueExact();
    default:
      return delegate.readInt64();
    }
  }

  @Override
  public int nextInt() throws IOException {
    switch (delegate.getCurrentBsonType()) {
    case DOUBLE:
      return (int) delegate.readDouble();
    case INT64:
      return (int) delegate.readInt64();
    case DECIMAL128:
      return delegate.readDecimal128().bigDecimalValue().intValueExact();
    default:
      return delegate.readInt32();
    }
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public void skipValue() throws IOException {
    delegate.skipValue();
  }

}
