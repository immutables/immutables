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

import org.immutables.criteria.Criteria;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.Path;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

class MongoPathNaming implements PathNaming {

  @Override
  public String name(Path path) {
    Objects.requireNonNull(path, "path");
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


    return path.paths().stream().map(toStringFn).collect(Collectors.joining("."));
  }
}
