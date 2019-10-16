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

package org.immutables.criteria.mongo.bson4jackson;

import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoClientSettings;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.immutables.check.Checkers.check;

/**
 * Low-level parser tests.
 */
public class BsonParserTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void array() throws Exception {
    compare("[]");
    compare("[[]]");
    compare("[[[]]]");
    compare("[[], []]");
    compare("[[], [[]]]");
    compare("[[], [[]], []]");
    compare("[1]");
    compare("[1, 2]");
    compare("[1, 2, 3]");
    compare("[true]");
    compare("[true, true]");
    compare("[true, true, false]");
    compare("[0.11, 11.22, 3]");
    compare("[\"foo\"]");
    compare("[\"\"]");
    compare("[\"\", \"\"]");
    compare("[\"\", \"foo\"]");
    compare("[\"foo\", \"bar\"]");
    compare("[1, true, 0, 1.111]");
    compare("[null]");
    compare("[null, 1, false]");
    compare("[0.0, -1.2, 3]");
    compare("[[0], [1]]");
    compare("[[0], [], 1]");
    compare("[true, [], []]");
    compare("[{}]");
    compare("[{}, {}]");
    compare("[{}, {}, {}]");
    compare("[{\"a\": 1}, {\"b\": null}, {\"c\": false}]");
    compare("[{\"0\": 1}, [], {\"1\": null}, {}]");
  }

  @Test
  public void scalar() throws Exception {
    compare("0");
    compare("0.0");
    compare("-1");
    compare("-200");
    compare(Long.toString(Long.MIN_VALUE));
    compare(Long.toString(Long.MAX_VALUE));
    compare(Integer.toString(Integer.MIN_VALUE));
    compare(Integer.toString(Integer.MAX_VALUE));
    compare(Byte.toString(Byte.MIN_VALUE));
    compare(Byte.toString(Byte.MAX_VALUE));
    compare(Short.toString(Short.MIN_VALUE));
    compare(Short.toString(Short.MAX_VALUE));
    compare("0.1");
    compare("-0.1111");
    compare("-2.222");
    compare("0.11111111111");
    compare("true");
    compare("false");
    compare("null");
    compare("\"foo\"");
    compare("\"\"");
    compare("\"null\"");
  }

  @Test
  public void object() throws Exception {
    compare("{}");
    compare("{\"foo\": \"bar\"}");
    compare("{\"foo\": 1}");
    compare("{\"foo\": true}");
    compare("{\"foo\": 0.1}");
    compare("{\"foo\": null}");
    compare("{\"foo\": {}}");
    compare("{\"foo\": []}");
    compare("{\"foo\": [{}]}");
    compare("{\"foo\": [{}, {}]}");
    compare("{\"foo\": [1, 2, 3]}");
    compare("{\"foo\": [null]}");
    compare("{\"foo\": \"\"}");
    compare("{\"foo\": \"2017-09-09\"}");
    compare("{\"foo\": {\"bar\": \"qux\"}}");
    compare("{\"foo\": 1, \"bar\": 2}");
    compare("{\"foo\": [], \"bar\": {}}");
    compare("{\"foo\": {\"bar\": {\"baz\": true}}}");
  }

  /**
   * Performs several json read / writes in binary format
   */
  private void compare(String json) throws IOException {
    jacksonThenBson(json);
    bsonThenJackson(json);
    jacksonThenJackson(json);
  }

  /**
   * write with Jackson read with Bson.
   * Inverse of {@link #bsonThenJackson(String)}
   */
  private void jacksonThenBson(String json) throws IOException {
    ObjectNode toWrite = maybeWrap(mapper.readTree(json));

    BasicOutputBuffer buffer = new BasicOutputBuffer();
    BsonWriter writer = new BsonBinaryWriter(buffer);
    BsonGenerator generator = new BsonGenerator(0, writer);
    // write with jackson
    mapper.writeValue(generator, toWrite);
    BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(buffer.toByteArray()));

    // read with BSON
    BsonDocument actual = MongoClientSettings.getDefaultCodecRegistry()
            .get(BsonDocument.class)
            .decode(reader, DecoderContext.builder().build());

    // compare results
    BsonDocument expected = BsonDocument.parse(toWrite.toString());
    if (!expected.equals(actual)) {
      check(maybeUnwrap(actual)).is(maybeUnwrap(expected));
      Assertions.fail("Should have failed before");
    }
  }

  /**
   * write with BSON read with jackson.
   * inverse of {@link #jacksonThenBson(String)}
   */
  private void bsonThenJackson(String json) throws IOException {
    ObjectNode toWrite = maybeWrap(mapper.readTree(json));

    BasicOutputBuffer buffer = new BasicOutputBuffer();
    BsonWriter writer = new BsonBinaryWriter(buffer);

    // write with BSON
    BsonDocument expected = BsonDocument.parse(toWrite.toString());
    MongoClientSettings.getDefaultCodecRegistry().get(BsonDocument.class)
            .encode(writer, expected, EncoderContext.builder().build());

    BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(buffer.toByteArray()));
    IOContext ioContext = new IOContext(new BufferRecycler(), null, false);
    BsonParser parser = new BsonParser(ioContext, 0, reader);

    // read with jackson
    BsonDocument actual = BsonDocument.parse(mapper.readValue(parser, JsonNode.class).toString());

    if (!actual.equals(expected)) {
       check(maybeUnwrap(actual)).is(maybeUnwrap(expected));
       Assertions.fail("Should have failed before");
    }
  }

  /**
   * Read and Write in Jackson API but using BSON reader/writer adapters
   */
  private void jacksonThenJackson(String json) throws IOException {
    ObjectNode expected = maybeWrap(mapper.readTree(json));

    BasicOutputBuffer buffer = new BasicOutputBuffer();
    BsonWriter writer = new BsonBinaryWriter(buffer);

    BsonGenerator generator = new BsonGenerator(0, writer);
    // write with Jackson
    mapper.writeValue(generator, expected);

    BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(buffer.toByteArray()));
    IOContext ioContext = new IOContext(new BufferRecycler(), null, false);
    BsonParser parser = new BsonParser(ioContext, 0, reader);

    // read with Jackson
    JsonNode actual = mapper.readValue(parser, JsonNode.class);
    check(actual).is(expected);
  }

  private ObjectNode maybeWrap(JsonNode toChange)  {
    // BSON likes encoding full document (not simple elements like BsonValue)
    if (!toChange.isObject()) {
      toChange = mapper.createObjectNode().set("ignore", toChange);
    }

    return (ObjectNode) toChange;
  }

  private BsonValue maybeUnwrap(BsonDocument document) {
    return document.containsKey("ignore") ? document.get("ignore") : document;
  }
}
