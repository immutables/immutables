/*
   Copyright 2013-2018 Immutables Authors and Contributors

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
import org.bson.types.Decimal128;
import org.immutables.mongo.Wrapper;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapter of {@link com.google.gson.stream.JsonWriter GSON Writer} writing JSON documents in <a href="http://bsonspec.org/">BSON format</a>
 * It delegates most of the calls to {@link org.bson.BsonWriter} which can serialize document representation in BSON binary version (default wire protocol
 * between mongo server and client). This allows to write / persist immutable objects directly into binary, bypassing intermediate
 * object representation (typically Immutable -> DBObject-> byte[]} ). Generated Gson {@link com.google.gson.TypeAdapter} takes care of
 * reading the object and calling write methods as a stream API.
 *
 * @see BsonReader
 * @see <a href="http://mongodb.github.io/mongo-java-driver/3.5/bson/">Mongo Driver (BSON)</a>
 * @see <a href="http://bsonspec.org/">BSON spec</a>
 */
@NotThreadSafe
public class BsonWriter extends com.google.gson.stream.JsonWriter implements Wrapper<org.bson.BsonWriter> {

  private static final Writer UNWRITABLE_WRITER = new Writer() {
    @Override public void write(char[] buffer, int offset, int counter) {
      throw new AssertionError();
    }
    @Override public void flush() throws IOException {
      throw new AssertionError();
    }
    @Override public void close() throws IOException {
      throw new AssertionError();
    }
  };

  private final org.bson.BsonWriter delegate;

  BsonWriter(org.bson.BsonWriter delegate) {
    super(UNWRITABLE_WRITER);
    this.delegate = checkNotNull(delegate, "delegate");
  }

  @Override
  public org.bson.BsonWriter unwrap() {
    return this.delegate;
  }

  @Override
  public com.google.gson.stream.JsonWriter beginArray() throws IOException {
    delegate.writeStartArray();
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter endArray() throws IOException {
    delegate.writeEndArray();
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter beginObject() throws IOException {
    delegate.writeStartDocument();
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter endObject() throws IOException {
    delegate.writeEndDocument();
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter name(String name) throws IOException {
    delegate.writeName(name);
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter value(String value) throws IOException {
    delegate.writeString(value);
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter jsonValue(String value) throws IOException {
    throw new UnsupportedOperationException("Can't write directly JSON to BSON");
  }

  @Override
  public com.google.gson.stream.JsonWriter nullValue() throws IOException {
    delegate.writeNull();
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter value(boolean value) throws IOException {
    delegate.writeBoolean(value);
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter value(Boolean value) throws IOException {
    if (value == null) {
      delegate.writeNull();
    } else {
      delegate.writeBoolean(value);
    }

    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter value(double value) throws IOException {
    delegate.writeDouble(value);
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter value(long value) throws IOException {
    delegate.writeInt64(value);
    return this;
  }

  public com.google.gson.stream.JsonWriter value(int value) {
    delegate.writeInt32(value);
    return this;
  }

  @Override
  public com.google.gson.stream.JsonWriter value(Number value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    if (value instanceof Double) {
      return value(value.doubleValue());
    }
    if (value instanceof Float) {
			return value(value.floatValue());
    }
    if (value instanceof Long){
      return value(value.longValue());
    }
    if (value instanceof Integer) {
      return value(value.intValue());
    }
    if (value instanceof Short) {
      return value(value.shortValue());
    }
    if (value instanceof Byte) {
     return value(value.byteValue());
    }
    if (value instanceof LazilyParsedNumber) {
      return value(value.longValue());
    }
    if (value instanceof BigDecimal) {
      final BigDecimal decimal = (BigDecimal) value;
      try {
        return value(new Decimal128(decimal));
      } catch (NumberFormatException ex) {
        // fallback to serializing to string
        return value(decimal.toPlainString());
      }
    }
    if (value instanceof BigInteger) {
      final BigInteger integer = (BigInteger) value;
      try {
        // BigDecimal is a wrapper for BigInteger anyway
        BigDecimal decimal = new BigDecimal(integer);
        return value(new Decimal128(decimal));
      } catch (NumberFormatException ex) {
        // fallback to serializing to string
        return value(integer.toString());
      }
    }
    // by default we resort to floating point 
    return value(value.doubleValue());
  }

  private com.google.gson.stream.JsonWriter value(Decimal128 decimal) {
    delegate.writeDecimal128(decimal);
    return this;
  }

  @Override
  public void flush() throws IOException {
    delegate.flush();
  }

  @Override
  public void close() throws IOException {
    if (delegate instanceof Closeable) {
      ((Closeable) delegate).close();
    }
  }

}
