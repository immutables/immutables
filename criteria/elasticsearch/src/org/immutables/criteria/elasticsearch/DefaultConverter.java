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

package org.immutables.criteria.elasticsearch;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;

/**
 * Parse default result
 */
class DefaultConverter<T> implements JsonConverter<T> {

  private final ObjectMapper mapper;
  private final JavaType javaType;

  private DefaultConverter(ObjectMapper mapper, JavaType javaType) {
    this.mapper = mapper;
    this.javaType = javaType;
  }

  @SuppressWarnings("unchecked")
  @Override
  public T convert(JsonNode node) {
    return (T) mapper.convertValue(node, javaType);
  }

  static <T> JsonConverter<T> of(ObjectMapper mapper, Class<T> type) {
    return new DefaultConverter<>(mapper, mapper.getTypeFactory().constructType(type));
  }

  static <T> JsonConverter<T> of(ObjectMapper mapper, Type type) {
    return new DefaultConverter<>(mapper, mapper.getTypeFactory().constructType(type));
  }

}
