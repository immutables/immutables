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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.Objects;

/**
 * Tries to find native codec for BSON types (if supported). Avoid using jackson indirection if
 * reading / writing only BSON (eg. {@link org.bson.BsonDocument})
 */
class NativeCodecRegistry implements CodecRegistry  {

  private final ObjectMapper mapper;

  NativeCodecRegistry(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  @Override
  public <T> Codec<T> get(Class<T> clazz) {
    final JavaType javaType = mapper.getTypeFactory().constructType(clazz);

    try {
      JsonSerializer<?> ser = mapper.getSerializerProviderInstance().findValueSerializer(javaType);
      if (ser instanceof Wrapper) {
        @SuppressWarnings("unchecked")
        Codec<T> codec = ((Wrapper<Codec<T>>) ser).unwrap();
        return codec;
      }
    } catch (JsonMappingException e) {
      throw new CodecConfigurationException("Exception for " + javaType, e);
    }

    throw new CodecConfigurationException(javaType + " not supported");
  }
}
