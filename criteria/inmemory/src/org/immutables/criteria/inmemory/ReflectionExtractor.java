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

import org.immutables.criteria.expression.Path;
import org.immutables.criteria.reflect.MemberExtractor;

import javax.annotation.Nullable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.Objects;
import java.util.Optional;

class ReflectionExtractor implements PathExtractor {

  private final MemberExtractor extractor;

  ReflectionExtractor() {
    this.extractor = MemberExtractor.ofReflection();
  }

  @Override
  @Nullable
  public Object extract(Path path, Object instance) {
    Objects.requireNonNull(path, "path");

    Object result = instance;

    for (AnnotatedElement member: path.paths()) {
      result = extract((Member) member, result);
      result = maybeUnwrapOptional(result);
      if (result == null) {
        return null;
      }
    }

    return result;
  }

  private Object extract(Member member, Object instance) {
    if (instance == null) {
      return null;
    }

    return extractor.extract(member, instance);
  }

  @SuppressWarnings("unchecked")
  private static Object maybeUnwrapOptional(Object maybeOptional) {
    if ((maybeOptional instanceof Optional)) {
      return  ((Optional) maybeOptional).orElse(null);
    }

    if (maybeOptional instanceof com.google.common.base.Optional) {
      return ((com.google.common.base.Optional) maybeOptional).orNull();
    }

    return maybeOptional;
  }
}
