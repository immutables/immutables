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

import org.immutables.criteria.Criteria;

import java.lang.reflect.Member;
import java.util.function.Function;

/**
 * Extracts value which represents unique ID for an instance.
 *
 * @param <T> instance type
 * @param <ID> ID attribute type
 */
public interface IdExtractor<T, ID> {

  ID extract(T instance);

  /**
   * Return function which extracts identifier (key) from existing instance.
   * Identifier is defined on POJOs with {@link Criteria.Id} annotation.
   * @throws IllegalArgumentException if {@link Criteria.Id} annotation is not declared in any methods
   * or fields.
   */
  @SuppressWarnings("unchecked")
  static <T, ID> IdExtractor<T, ID> ofMember(Class<T> entityType) {
    return from((Function<T, ID>) Reflections.extractorFor(IdResolver.defaultResolver().resolve(entityType)));
  }

  @SuppressWarnings("unchecked")
  static <T, ID> IdExtractor<T, ID> ofMember(Member member) {
    return from((Function<T, ID>) Reflections.extractorFor(member));
  }

  static <T, ID> IdExtractor<T, ID> from(Function<T, ID> fun) {
    return fun::apply;
  }

}
