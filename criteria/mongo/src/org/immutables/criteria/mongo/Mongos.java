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

package org.immutables.criteria.mongo;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.ExpressionConverter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Util methods for mongo adapter.
 */
final class Mongos {

  private Mongos() {}

  /**
   * Convert existing expression to Bson
   */
  static ExpressionConverter<Bson> converter(PathNaming pathNaming, CodecRegistry codecRegistry) {
    return expression -> expression.accept(new FindVisitor(pathNaming, codecRegistry));
  }

  /**
   * Check if current type represents an optional type like {@link OptionalLong}, {@link Optional} etc.
   */
  static boolean isOptional(Type type) {
    return type == Optional.class || type == OptionalDouble.class || type == OptionalLong.class || type == OptionalInt.class || (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() == Optional.class);
  }

}
