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
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.jsr310.Jsr310CodecProvider;

/**
 * Registers default BSON codecs like {@link ValueCodecProvider},  {@link BsonValueCodecProvider} and
 * {@link Jsr310CodecProvider}  so BSON types can be serialized / deserialized by Jackson in
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
    return CodecRegistries.fromProviders(new BsonValueCodecProvider(),
            new ValueCodecProvider(), new Jsr310CodecProvider());
  }

  @Override
  public void setupModule(SetupContext context) {
    context.addSerializers(JacksonCodecs.serializers(registry));
    context.addDeserializers(JacksonCodecs.deserializers(registry));
  }
}
