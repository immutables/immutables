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

import org.immutables.criteria.backend.ExpressionNaming;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.Path;

import java.lang.reflect.Member;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

class MongoPathNaming implements PathNaming {

  private final Member idProperty;

  MongoPathNaming(Path idPath) {
    this((Member) idPath.element());
  }

  MongoPathNaming(Member idProperty) {
    this.idProperty = idProperty;
  }

  @Override
  public String name(Path path) {
    Objects.requireNonNull(path, "path");
    Function<Member, String> toStringFn = a -> {
      Objects.requireNonNull(a, "null element");
      if (a.equals(idProperty)) {
        // is ID attribute ?
        return "_id";
      } else {
        return a.getName();
      }
    };

    return path.members().stream().map(toStringFn).collect(Collectors.joining("."));
  }

  ExpressionNaming toExpression() {
    return expression -> MongoPathNaming.this.name((Path) expression);
  }
}
