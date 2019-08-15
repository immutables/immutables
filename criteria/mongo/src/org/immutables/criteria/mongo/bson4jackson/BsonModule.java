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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import org.bson.codecs.BigDecimalCodec;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.ByteArrayCodec;
import org.bson.codecs.DateCodec;
import org.bson.codecs.Decimal128Codec;
import org.bson.codecs.ObjectIdCodec;
import org.bson.codecs.PatternCodec;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.jsr310.Jsr310CodecProvider;

/*
 * Registers default BSON codecs like {@link ValueCodecProvider} or
 * {@link Jsr310CodecProvider} so BSON types can be serialized / deserialized by Jackson in
 * native format.
 */
public class BsonModule extends Module {

  private final CodecRegistry registry;

  public BsonModule() {
    this(defaultRegistry());
  }

  private BsonModule(CodecRegistry registry) {
    this.registry = registry;
  }

  @Override
  public String getModuleName() {
    return BsonModule.class.getSimpleName();
  }

  @Override
  public Version version() {
    return Version.unknownVersion();
  }

  private static CodecRegistry defaultRegistry() {
    CodecRegistry standard = CodecRegistries.fromProviders(
            new BsonValueCodecProvider(),
            new Jsr310CodecProvider());

    // avoid codecs for String / Long / Boolean etc. They're already handled by jackson
    // choose the ones which need to be serialized in non-JSON format (BSON)
    CodecRegistry others = CodecRegistries.fromCodecs(new ObjectIdCodec(),
            new DateCodec(), new UuidCodec(), new Decimal128Codec(),
            new PatternCodec(),
            new BigDecimalCodec(), new ByteArrayCodec());

    return CodecRegistries.fromRegistries(standard, others);
  }

  @Override
  public void setupModule(SetupContext context) {
    context.addSerializers(JacksonCodecs.serializers(registry));
    context.addDeserializers(JacksonCodecs.deserializers(registry));
  }
}
