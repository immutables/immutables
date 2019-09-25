/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.mongo.bson4gson;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonObjectId;
import org.bson.BsonRegularExpression;
import org.bson.BsonTimestamp;
import org.bson.BsonValue;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.io.IOException;

import static org.immutables.check.Checkers.check;

/**
 * Validate type conversion between BSON and gson-JSON ({@link com.google.gson.stream.JsonToken}).
 */
public class TypeConversionTest {

  @Test
  public void int32() throws IOException {
    check(readerFor(new BsonInt32(42)).nextInt()).is(42);
    check(readerFor(new BsonInt32(42)).nextLong()).is(42L);
    check(readerFor(new BsonInt32(42)).nextDouble()).is(42D);
    check(readerFor(new BsonInt32(42)).nextString()).is("42");
  }

  @Test
  public void int64() throws IOException {
    check(readerFor(new BsonInt64(64)).nextInt()).is(64);
    check(readerFor(new BsonInt64(64)).nextLong()).is(64L);
    check(readerFor(new BsonInt64(64)).nextDouble()).is(64D);
    check(readerFor(new BsonInt64(64)).nextString()).is("64");
  }

  @Test
  public void bsonDouble() throws IOException {
    check(readerFor(new BsonDouble(1.1)).nextInt()).is(1);
    check(readerFor(new BsonDouble(1.1)).nextLong()).is(1L);
    check(readerFor(new BsonDouble(1.1)).nextDouble()).is(1.1D);
    check(readerFor(new BsonDouble(1.1)).nextString()).is(Double.toString(1.1));
  }

  @Test
  public void dateTime() throws IOException {
    final long epoch = System.currentTimeMillis();
    check(readerFor(new BsonDateTime(epoch)).nextInt()).is((int) epoch);
    check(readerFor(new BsonDateTime(epoch)).nextLong()).is(epoch);
    check(readerFor(new BsonDateTime(epoch)).nextDouble()).is((double) epoch);
  }

  @Test
  public void timestamp() throws IOException {
    final long epoch = System.currentTimeMillis();
    check(readerFor(new BsonTimestamp(epoch)).nextInt()).is((int) epoch);
    check(readerFor(new BsonTimestamp(epoch)).nextLong()).is(epoch);
    check(readerFor(new BsonTimestamp(epoch)).nextDouble()).is((double) epoch);
  }

  @Test
  public void regexpPattern() throws IOException {
    check(readerFor(new BsonRegularExpression("abc")).nextString()).is("abc");
    check(readerFor(new BsonRegularExpression(".*")).nextString()).is(".*");
  }

  @Test
  public void objectId() throws IOException {
    ObjectId id = ObjectId.get();
    check(readerFor(new BsonObjectId(id)).nextString()).is(id.toHexString());
  }

  /**
   * Creates reader for provided bson {@code value}
   */
  private static JsonReader readerFor(BsonValue value) throws IOException {
    BsonDocument doc = new BsonDocument("value", value);
    BsonReader reader = new BsonReader(new BsonDocumentReader(doc));
    // advance AFTER value token
    reader.beginObject();
    check(reader.peek()).is(JsonToken.NAME);
    check(reader.nextName()).is("value");
    return reader;
  }

}
