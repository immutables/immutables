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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.immutables.check.Checkers.check;

/**
 * Low-level writer (JsonGenerator) tests
 */
class BsonGeneratorTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void emptyObject() throws IOException {
    BsonDocumentWriter writer = new BsonDocumentWriter(new BsonDocument());
    BsonGenerator generator = new BsonGenerator(0, mapper, writer);
    generator.writeStartObject();
    generator.writeEndObject();

    check(writer.getDocument().keySet()).isEmpty();
    check(writer.getDocument()).is(new BsonDocument());
  }

  @Test
  void checkClosed() throws IOException {
    BsonDocumentWriter writer = new BsonDocumentWriter(new BsonDocument());
    BsonGenerator generator = new BsonGenerator(0, mapper, writer);

    check(!generator.isClosed());
    generator.close();
    check(generator.isClosed());
  }

  /**
   * Writing null values does not cause an exception
   */
  @Test
  void nulls() throws IOException {
    BsonDocumentWriter writer = new BsonDocumentWriter(new BsonDocument());
    BsonGenerator generator = new BsonGenerator(0, mapper, writer);
    generator.writeStartObject();

    generator.writeFieldName("null");
    generator.writeNull();

    generator.writeNullField("nullField");

    generator.writeFieldName("string");
    generator.writeString((String) null);

    generator.writeFieldName("bigDecimal");
    generator.writeNumber((BigDecimal) null);

    generator.writeFieldName("bigInteger");
    generator.writeNumber((BigInteger) null);
    generator.writeEndObject();
    BsonDocument doc = writer.getDocument();
    check(doc.get("null")).is(BsonNull.VALUE);
    check(doc.get("nullField")).is(BsonNull.VALUE);
    check(doc.get("string")).is(BsonNull.VALUE);
    check(doc.get("bigDecimal")).is(BsonNull.VALUE);
    check(doc.get("bigInteger")).is(BsonNull.VALUE);
  }


}