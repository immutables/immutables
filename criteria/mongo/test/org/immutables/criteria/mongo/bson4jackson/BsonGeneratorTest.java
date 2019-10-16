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

import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonNull;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.immutables.check.Checkers.check;

/**
 * Low-level writer (JsonGenerator) tests
 */
class BsonGeneratorTest {

  @Test
  void emptyObject() throws IOException {
    BsonDocumentWriter writer = new BsonDocumentWriter(new BsonDocument());
    BsonGenerator generator = generatorFor(writer);
    generator.writeStartObject();
    generator.writeEndObject();

    check(writer.getDocument().keySet()).isEmpty();
    check(writer.getDocument()).is(new BsonDocument());
  }

  @Test
  void binary() throws IOException {
    BsonDocumentWriter writer = new BsonDocumentWriter(new BsonDocument());
    BsonGenerator generator = generatorFor(writer);
    check(generator.canWriteBinaryNatively());
    check(writeAndReturnValue(gen -> gen.writeBinary(new byte[] {})).asBinary().getData()).isEmpty();
    check(writeAndReturnValue(gen -> gen.writeBinary(new byte[] {1})).asBinary().getData()).isOf((byte) 1);
    check(writeAndReturnValue(gen -> gen.writeBinary(new byte[] {1, 2})).asBinary().getData()).isOf((byte) 1, (byte) 2);
    check(writeAndReturnValue(gen -> gen.writeBinary(new byte[] {1, 2, 3})).asBinary().getData()).isOf((byte) 1, (byte) 2, (byte) 3);
  }

  @Test
  void checkClosed() throws IOException {
    BsonDocumentWriter writer = new BsonDocumentWriter(new BsonDocument());
    BsonGenerator generator = generatorFor(writer);

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
    BsonGenerator generator = generatorFor(writer);
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

  private BsonValue writeAndReturnValue(IoConsumer<BsonGenerator> consumer) throws IOException {
    BsonDocumentWriter writer = new BsonDocumentWriter(new BsonDocument());
    BsonGenerator generator = generatorFor(writer);
    generator.writeStartObject();
    generator.writeFieldName("value");
    consumer.accept(generator);
    generator.writeEndObject();
    BsonDocument doc = writer.getDocument();
    check(doc.keySet()).has("value");
    return doc.get("value");
  }

  private interface IoConsumer<T> {
    void accept(T value) throws IOException;
  }

  private BsonGenerator generatorFor(BsonWriter writer) {
    return new BsonGenerator(0, writer);
  }


}