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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.mongodb.client.model.Filters;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonRegularExpression;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.io.BasicOutputBuffer;
import org.bson.types.ObjectId;
import org.immutables.value.Value;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.immutables.check.Checkers.check;

public class JacksonCodecsTest {

  /**
   * Test that regular expression is correctly encoded
   */
  @Test
  public void regex() {
    final ObjectMapper mapper = new ObjectMapper().registerModule(new BsonModule());
    final CodecRegistry registry = JacksonCodecs.registryFromMapper(mapper);

    Consumer<Bson> validate = bson -> {
      BsonDocument doc = bson.toBsonDocument(BsonDocument.class, registry);
      check(doc.get("a")).is(new BsonRegularExpression("a.*b"));
    };

    validate.accept(Filters.eq("a", Pattern.compile("a.*b")));
    validate.accept(Filters.regex("a", Pattern.compile("a.*b")));
    validate.accept(Filters.regex("a", "a.*b"));
  }

  @Test
  public void encodeDecode() throws IOException {
    BsonModel model = ImmutableBsonModel.builder().utilDate(new java.util.Date())
            .localDate(LocalDate.now())
            .pattern(Pattern.compile("a.*b"))
            .objectId(ObjectId.get())
            .putMap("key1", "val1")
            .build();

    final ObjectMapper mapper = new ObjectMapper().registerModule(new BsonModule())
            .registerModule(new GuavaModule());
    final CodecRegistry registry = JacksonCodecs.registryFromMapper(mapper);

    BasicOutputBuffer buffer = new BasicOutputBuffer();
    BsonWriter writer = new BsonBinaryWriter(buffer);

    registry.get(BsonModel.class).encode(writer, model, EncoderContext.builder().build());

    BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(buffer.toByteArray()));
    IOContext ioContext = new IOContext(new BufferRecycler(), null, false);
    BsonParser parser = new BsonParser(ioContext, 0, reader);

    // read
    BsonModel model2 = mapper.readValue(parser, BsonModel.class);

    check(model2.localDate()).is(model.localDate());
    check(model2.utilDate()).is(model.utilDate());
    check(model2.objectId()).is(model.objectId());
    check(model2.map().keySet()).isOf("key1");
    check(model2.map()).is(model.map());
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableBsonModel.class)
  @JsonDeserialize(as = ImmutableBsonModel.class)
  interface BsonModel {
      java.util.Date utilDate();
      LocalDate localDate();
      Pattern pattern();
      ObjectId objectId();
      Map<String, String> map();
  }

}