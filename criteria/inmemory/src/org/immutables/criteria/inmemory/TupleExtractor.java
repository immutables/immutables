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

package org.immutables.criteria.inmemory;

import com.google.common.base.Preconditions;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

class TupleExtractor  {

  private final Query query;
  private final PathExtractor pathExtractor;

  TupleExtractor(Query query, PathExtractor pathExtractor) {
    Preconditions.checkArgument(query.hasProjections(), "no projections defined");
    this.query = query;
    this.pathExtractor = pathExtractor;
  }

  ProjectedTuple extract(Object instance) {
    List<Object> values = new ArrayList<>();
    for(Expression expr: query.projections()) {
      Path path = (Path) expr;
      Object value = pathExtractor.extract(path, instance);
      value = maybeWrapOptional(value, path);
      values.add(value);
    }

    return ProjectedTuple.of(query.projections(), values);
  }

  /**
   * Expected result might be optional
   */
  private static Object maybeWrapOptional(Object value, Path path) {
    Type type = path.returnType();
    final Class<?> klass;
    if (type instanceof ParameterizedType) {
      klass = (Class<?>) ((ParameterizedType) type).getRawType();
    } else if (type instanceof Class) {
      klass = (Class<?>) type;
    } else {
      throw new IllegalArgumentException("Unknown type " + type + " for path " + path.toStringPath());
    }

    // don't wrap if already optional
    if (value instanceof OptionalDouble || value instanceof OptionalInt
            || value instanceof OptionalLong || value instanceof Optional || value instanceof com.google.common.base.Optional) {
      return value;
    }

    if (Optional.class.isAssignableFrom(klass)) {
      return Optional.ofNullable(value);
    } else if (OptionalDouble.class.isAssignableFrom(klass)) {
      return value == null ? OptionalDouble.empty() : OptionalDouble.of((Double) value);
    } else if (OptionalLong.class.isAssignableFrom(klass)) {
      return value == null ? OptionalLong.empty() : OptionalLong.of((Long) value);
    } else if (OptionalInt.class.isAssignableFrom(klass)) {
      return value == null ? OptionalInt.empty() : OptionalInt.of((Integer) value);
    } else if (com.google.common.base.Optional.class.isAssignableFrom(klass)) {
      return com.google.common.base.Optional.fromNullable(value);
    }

    return value;
  }


}
