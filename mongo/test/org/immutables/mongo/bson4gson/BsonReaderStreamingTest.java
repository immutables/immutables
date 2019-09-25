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
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonInt32;
import org.junit.Test;

import java.io.IOException;

import static org.immutables.check.Checkers.check;

/**
 * Check streaming API
 */
public class BsonReaderStreamingTest {

  @Test
  public void object() throws IOException {
    JsonReader reader = createReader("{}");
    check(reader.peek()).is(JsonToken.BEGIN_OBJECT);
    reader.beginObject();
    check(reader.peek()).is(JsonToken.END_OBJECT);
    check(!reader.hasNext());
    reader.endObject();
    check(!reader.hasNext());
  }

  @Test
  public void singleValue() throws IOException {
    BsonDocument document = new BsonDocument();
    document.append("value", new BsonInt32(42));

    JsonReader reader = createReader(document);
    check(reader.peek()).is(JsonToken.BEGIN_OBJECT);
    reader.beginObject();
    check(reader.hasNext());
    check(reader.peek()).is(JsonToken.NAME);
    check(reader.nextName()).is("value");
    check(reader.peek()).is(JsonToken.NUMBER);
    check(reader.nextInt()).is(42);
    check(reader.peek()).is(JsonToken.END_OBJECT);
    reader.endObject();
    check(!reader.hasNext());
  }

  @Test
  public void value2() throws IOException {
    JsonReader reader  = createReader("{ \"a\" : true, \"b\": \"foo\" }");
    check(reader.hasNext());
    reader.beginObject();
    check(reader.hasNext());
    check(reader.peek()).is(JsonToken.NAME);
    check(reader.nextName()).is("a");
    check(reader.peek()).is(JsonToken.BOOLEAN);
    check(reader.nextBoolean());
    check(reader.hasNext());
    check(reader.peek()).is(JsonToken.NAME);
    check(reader.nextName()).is("b");
    check(reader.peek()).is(JsonToken.STRING);
    check(reader.nextString()).is("foo");
    check(reader.peek()).is(JsonToken.END_OBJECT);
    reader.endObject();
  }

  @Test
  public void value3() throws IOException {
    JsonReader reader = createReader("{ \"a\" : 123, \"list\" : [ 12.25, null, true, { }, [ ] ] }");

    reader.beginObject();
    check(reader.nextName()).is("a");
    check(reader.nextLong()).is(123L);
    check(reader.nextName()).is("list");
    check(reader.peek()).is(JsonToken.BEGIN_ARRAY);
    reader.beginArray();
    check(reader.peek()).is(JsonToken.NUMBER);
    check(reader.nextDouble()).is(12.25);
    check(reader.peek()).is(JsonToken.NULL);
    reader.nextNull();
    check(reader.peek()).is(JsonToken.BOOLEAN);
    check(reader.nextBoolean());
    check(reader.peek()).is(JsonToken.BEGIN_OBJECT);
    reader.beginObject();
    check(reader.peek()).is(JsonToken.END_OBJECT);
    reader.endObject();
    check(reader.peek()).is(JsonToken.BEGIN_ARRAY);
    reader.beginArray();
    check(reader.peek()).is(JsonToken.END_ARRAY);
    reader.endArray();
    check(reader.peek()).is(JsonToken.END_ARRAY);
    reader.endArray();
    check(reader.peek()).is(JsonToken.END_OBJECT);
    reader.endObject();
  }

  private static JsonReader createReader(String json) {
    return createReader(BsonDocument.parse(json));
  }

  private static JsonReader createReader(BsonDocument document) {
    return new BsonReader(new BsonDocumentReader(document));
  }
}
