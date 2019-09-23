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

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.mongodb.MongoClientSettings;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.io.BasicOutputBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Check {@link BsonParser} call by call using low-level stream API
 */
class BsonParserTreeTest {

  private static BsonParser createParser(String json) {
    Objects.requireNonNull(json, "json");
    BasicOutputBuffer buffer = new BasicOutputBuffer();
    BsonDocument bson = BsonDocument.parse(json);
    CodecRegistry registry = MongoClientSettings.getDefaultCodecRegistry();
    registry.get(BsonDocument.class)
            .encode(new BsonBinaryWriter(buffer), bson, EncoderContext.builder().build());

    BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(buffer.toByteArray()));
    IOContext ioContext = new IOContext(new BufferRecycler(), null, false);
    return new BsonParser(ioContext, 0, reader);
  }

  @Test
  void getText() throws IOException {
    BsonParser p = createParser("{ \"a\" : true, \"b\": \"foo\" }");
    assertNull(p.currentToken());
    assertNull(p.getCurrentName());
    assertNull(p.getText());
    assertToken(JsonToken.START_OBJECT, p.nextToken());
    assertToken(JsonToken.START_OBJECT, p.currentToken());
    assertToken(JsonToken.START_OBJECT, p.currentToken()); // read same token multiple times
    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertToken(JsonToken.FIELD_NAME, p.currentToken());
    assertToken(JsonToken.FIELD_NAME, p.currentToken()); // read same token multiple times
    assertEquals("a", p.getCurrentName());
    assertEquals("a", p.getCurrentName()); // read current name multiple times
    assertEquals("a", p.getText()); // read same text multiple times
    assertEquals("a", p.getText());

    assertToken(JsonToken.VALUE_TRUE, p.nextToken());
    assertToken(JsonToken.VALUE_TRUE, p.currentToken()); // read same token multiple times
    assertToken(JsonToken.VALUE_TRUE, p.currentToken());
    assertEquals(JsonToken.VALUE_TRUE.asString(), p.getText());
    assertEquals(JsonToken.VALUE_TRUE.asString(), p.getText()); // as text multiple times

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("b", p.getCurrentName());
    assertEquals("b", p.getCurrentName());
    assertToken(JsonToken.VALUE_STRING, p.nextToken());
    assertEquals("foo", p.getText());
    assertEquals("foo", p.getText()); // read same text multiple times
    assertEquals("foo", p.getText());
    assertToken(JsonToken.END_OBJECT, p.nextToken());
    assertNull(p.nextToken());
    assertNull(p.nextToken());
    p.close();
    assertTrue(p.isClosed());
  }

  /**
   * Taken from jackson codebase: TestTreeTraversingParser
   */
  @Test
  void streamRead() throws Exception {
    BsonParser p = createParser("{ \"a\" : 123, \"list\" : [ 12.25, null, true, { }, [ ] ] }");

    assertNull(p.currentToken());
    assertNull(p.getCurrentName());

    assertToken(JsonToken.START_OBJECT, p.nextToken());
    assertNull(p.getCurrentName());
//    assertEquals("Expected START_OBJECT", JsonToken.START_OBJECT.asString(), p.getText());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("a", p.getCurrentName());
    assertEquals("a", p.getText());

    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
    assertEquals("a", p.getCurrentName());
    assertEquals(123, p.getIntValue());
    assertEquals(123, p.getLongValue()); // call couple of times to make sure value is cached
    assertEquals(123D, p.getDoubleValue(), 0); // call couple of times
    assertEquals(BigDecimal.valueOf(123L), p.getDecimalValue()); // call couple of times
    assertEquals(BigInteger.valueOf(123L), p.getBigIntegerValue()); // call couple of times
    assertEquals("123", p.getText());

    assertToken(JsonToken.FIELD_NAME, p.nextToken());
    assertEquals("list", p.getCurrentName());
    assertEquals("list", p.getText());

    assertToken(JsonToken.START_ARRAY, p.nextToken());
//    assertEquals("list", p.getCurrentName());
//    assertEquals(JsonToken.START_ARRAY.asString(), p.getText());

    assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
//    assertNull(p.getCurrentName());
    assertEquals(12.25, p.getDoubleValue(), 0);
    assertEquals(12, p.getIntValue());
    assertEquals(12, p.getLongValue());
    assertEquals(BigInteger.valueOf(12), p.getBigIntegerValue());
    assertEquals(BigDecimal.valueOf(12.25D), p.getDecimalValue());
    assertEquals("12.25", p.getText());

    assertToken(JsonToken.VALUE_NULL, p.nextToken());
    assertNull(p.getCurrentName());
    assertEquals(JsonToken.VALUE_NULL.asString(), p.getText());

    assertToken(JsonToken.VALUE_TRUE, p.nextToken());
    assertNull(p.getCurrentName());
    assertTrue(p.getBooleanValue());
    assertEquals(JsonToken.VALUE_TRUE.asString(), p.getText());

    assertToken(JsonToken.START_OBJECT, p.nextToken());
    assertNull(p.getCurrentName());
    assertToken(JsonToken.END_OBJECT, p.nextToken());
    assertNull(p.getCurrentName());

    assertToken(JsonToken.START_ARRAY, p.nextToken());
    assertNull(p.getCurrentName());
    assertToken(JsonToken.END_ARRAY, p.nextToken());
    assertNull(p.getCurrentName());

    assertToken(JsonToken.END_ARRAY, p.nextToken());

    assertToken(JsonToken.END_OBJECT, p.nextToken());
    assertNull(p.getCurrentName());

    assertNull(p.nextToken());

    p.close();
    assertTrue(p.isClosed());
  }

  private static void assertToken(JsonToken expToken, JsonToken actToken) {
    if (actToken != expToken) {
      Assertions.fail("Expected token " + expToken + ", current token " + actToken);
    }
  }

}
