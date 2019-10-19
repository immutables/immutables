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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.mongodb.MongoClientSettings;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.io.BasicOutputBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.immutables.check.Checkers.check;

/**
 * Util and helper methods
 */
final class Parsers {

  private Parsers() {}

  static BsonParser createParser(BsonDocument bson) {
    BasicOutputBuffer buffer = new BasicOutputBuffer();
    CodecRegistry registry = MongoClientSettings.getDefaultCodecRegistry();
    registry.get(BsonDocument.class)
            .encode(new BsonBinaryWriter(buffer), bson, EncoderContext.builder().build());

    BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(buffer.toByteArray()));
    IOContext ioContext = new IOContext(new BufferRecycler(), null, false);
    return new BsonParser(ioContext, 0, reader);
  }

  static BsonParser createParser(String json) {
    return createParser(BsonDocument.parse(json));
  }

  /**
   * Create parser positioned at {@code value}
   */
  static JsonParser parserAt(BsonValue value) throws IOException {
    JsonParser parser = createParser(new BsonDocument("value", value));
    parser.nextToken();
    check(parser.nextFieldName()).is("value");
    parser.nextToken();
    return parser;
  }
}
