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

import com.google.common.base.Function;
import org.bson.conversions.Bson;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.expression.ExpressionConverter;
import org.immutables.criteria.expression.Path;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Util methods for mongo adapter.
 */
final class Mongos {

  private Mongos() {}

  /**
   * Convert existing expression to Bson
   */
  static ExpressionConverter<Bson> converter() {
    return expression -> expression.accept(new MongoQueryVisitor());
  }

  static String toMongoFieldName(Path path) {
    Function<AnnotatedElement, String> toStringFn = a -> {
      Objects.requireNonNull(a, "null element");
      if (a.isAnnotationPresent(Criteria.Id.class)) {
        return "_id";
      } else if (a instanceof Member) {
        return ((Member) a).getName();
      } else if (a instanceof Class) {
        return ((Class) a).getSimpleName();
      }

      throw new IllegalArgumentException("Don't know how to name " + a);
    };


    return path.paths().stream().map(toStringFn::apply).collect(Collectors.joining("."));
  }
}
