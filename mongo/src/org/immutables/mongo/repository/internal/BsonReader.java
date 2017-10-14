/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.mongo.repository.internal;

import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.bson.AbstractBsonReader;
import org.bson.AbstractBsonReader.State;
import org.bson.BsonType;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapter of {@link JsonReader GSON Reader} reading directly from <a href="http://bsonspec.org/">BSON encoded</a> documents.
 * It delegates most of the calls to {@link org.bson.BsonReader} which understands binary JSON representation (default wire protocol
 * between mongo server and client). This class allows to instantiate immutable objects directly from binary stream (eg. {@code byte[]}) bypassing intermediate
 * object instantiation (typically {@code byte[] -> DBObject-> Immutable} ). Internally generated GSON {@link com.google.gson.TypeAdapter} will
 * read incoming bytes as stream API.
 *
 * @see BsonWriter
 * @see <a href="http://mongodb.github.io/mongo-java-driver/3.5/bson/">Mongo Driver (BSON)</a>
 * @see <a href="http://bsonspec.org/">BSON spec</a>
 */
@NotThreadSafe
public class BsonReader extends JsonReader {

  private static final Reader UNREADABLE_READER = new Reader() {
    @Override public int read(char[] buffer, int offset, int count) throws IOException {
      throw new AssertionError();
    }
    @Override public void close() throws IOException {
      throw new AssertionError();
    }
  };

  private final AbstractBsonReader delegate;

  /**
   * In order to be able to get current location of the BSON "cursor" (document or array start/end) enforcing clients to
   * provide {@link AbstractBsonReader} which exposes this information via {@code getState()} method, instead of more generic interface {@link BsonReader}.
   *
   * @param delegate
   * @see AbstractBsonReader#getState()
   */
  BsonReader(org.bson.AbstractBsonReader delegate) {
    super(UNREADABLE_READER);
    this.delegate = checkNotNull(delegate, "delegate");
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
    if (!hasMoreElements()) return false;
    advance();
    return hasMoreElements();
  }

  private boolean hasMoreElements() {
    return !(state() == State.END_OF_DOCUMENT || state() == State.END_OF_ARRAY || state() == State.DONE);
  }

  @Override
  public JsonToken peek() throws IOException {
    JsonToken token = null;

    if (state() == State.INITIAL || state() == State.SCOPE_DOCUMENT) {
      advance();
      token = toGsonToken(delegate.getCurrentBsonType());
    } else if (state() == State.TYPE) {
      advance();
      token = toGsonToken(delegate.getCurrentBsonType());
    } else if (state() == State.NAME) {
      token = JsonToken.NAME;
    } else if (state() == State.END_OF_DOCUMENT) {
      token = JsonToken.END_OBJECT;
    } else if (state() == State.END_OF_ARRAY) {
      token = JsonToken.END_ARRAY;
    } else if (state() == State.DONE) {
      token = JsonToken.END_DOCUMENT;
    } else if (state() == State.VALUE) {
      token = toGsonToken(delegate.getCurrentBsonType());
    }

    if (token == null) {
      throw new IllegalStateException("Shouldn't get here (null token). Last state is " + state() + " currentType:" +
              delegate.getCurrentBsonType());
    }

    return token;
  }

  private State state() {
    return delegate.getState();
  }

  private static JsonToken toGsonToken(BsonType type) {
    final JsonToken token;
    switch (type) {
      case END_OF_DOCUMENT:
        token = JsonToken.END_DOCUMENT;
        break;
      case DOUBLE:
        token = JsonToken.NUMBER;
        break;
      case STRING:
        token = JsonToken.STRING;
        break;
      case DOCUMENT:
        token = JsonToken.BEGIN_OBJECT;
        break;
      case ARRAY:
        token = JsonToken.BEGIN_ARRAY;
        break;
      case OBJECT_ID:
        token = JsonToken.STRING;
        break;
      case BOOLEAN:
        token = JsonToken.BOOLEAN;
        break;
      case DATE_TIME:
        token = JsonToken.NUMBER;
        break;
      case NULL:
        token = JsonToken.NULL;
        break;
      case REGULAR_EXPRESSION:
        token = JsonToken.STRING;
        break;
      case SYMBOL:
        token = JsonToken.STRING;
        break;
      case INT32:
        token = JsonToken.NUMBER;
        break;
      case INT64:
        token = JsonToken.NUMBER;
        break;
      case TIMESTAMP:
        token = JsonToken.NUMBER;
        break;
      case DECIMAL128:
        token = JsonToken.NUMBER;
        break;
      case BINARY:
        token = JsonToken.STRING;
        break;
      default:
        // not really sure what to do with this type
        token = JsonToken.NULL;
    }

    return token;
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
   * Gson library reads numbers lazily when using generic {@link com.google.gson.internal.bind.TypeAdapters#JSON_ELEMENT} type adapter.
   * Number is read as string and then wrapped inside {@link LazilyParsedNumber}. This inefficiency should only occur if reading numbers with generic JSON element API
   * and not using generated type adapters.
   *
   * @see LazilyParsedNumber
   * @see com.google.gson.internal.bind.TypeAdapters#JSON_ELEMENT
   */
  private String scalarToString() throws IOException {
    final BsonType type = delegate.getCurrentBsonType();

    if (type == BsonType.STRING) {
      return delegate.readString();
    } else if (type == BsonType.SYMBOL) {
      return delegate.readSymbol();
    } else if (type == BsonType.INT32) {
      return Integer.toString(nextInt());
    } else if (type == BsonType.INT64) {
      return Long.toString(nextLong());
    } else if (type == BsonType.DOUBLE) {
      return Double.toString(nextDouble());
    }

    throw new IllegalStateException(String.format("Unknown scalar type to be converted to string: %s", type));
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
    return delegate.readDouble();
  }

  @Override
  public long nextLong() throws IOException {
    if (BsonType.INT32 == delegate.getCurrentBsonType()) {
      return delegate.readInt32();
    }
    return delegate.readInt64();
  }

  @Override
  public int nextInt() throws IOException {
    if (BsonType.INT64 == delegate.getCurrentBsonType()) {
      return (int) delegate.readInt64();
    }
    return delegate.readInt32();
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public void skipValue() throws IOException {
    delegate.skipValue();
  }

  public Pattern nextPattern() {
    return Pattern.compile(delegate.readRegularExpression().getPattern());
  }

  public Long nextTimeInstant() {
    return delegate.readDateTime();
  }

  public byte[] nextObjectId() {
    return delegate.readObjectId().toByteArray();
  }

  public byte[] nextBinary() {
    return delegate.readBinaryData().getData();
  }
}
