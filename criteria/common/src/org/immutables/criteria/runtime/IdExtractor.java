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

package org.immutables.criteria.runtime;

import java.lang.reflect.Member;
import java.util.Objects;
import java.util.function.Function;

/**
 * Extracts value which represents unique ID for an instance.
 */
public interface IdExtractor {

  Object extract(Object instance);

  /**
   * Return function which extracts id (key) value from existing instance using reflection API.
   * {@code ID} member (field or method) is identified using {@link IdResolver} strategy.
   * @param resolver id resolution strategy to apply when searching for {@code ID} attribute
   */
  static IdExtractor fromResolver(IdResolver resolver) {
    Objects.requireNonNull(resolver, "resolver");
    MemberExtractor extractor = MemberExtractor.ofReflection();
    return instance -> {
      Objects.requireNonNull(instance, "instance");
      Member member = resolver.resolve(instance.getClass());
      return extractor.extract(member, instance);
    };
  }

  static IdExtractor fromFunction(Function<Object, Object> fun) {
    return fun::apply;
  }
}
