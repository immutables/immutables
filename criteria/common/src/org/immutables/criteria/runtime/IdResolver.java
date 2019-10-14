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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Strategy to find {@code ID} attribute in a class.
 * Attribute can be either {@link Field} or {@link Method} and is usually a top-level element
 * in the class.
 */
public interface IdResolver {

  /**
   * Try to find member (field or method) of the class which represents {@code ID} attribute.
   *
   * @param type class where to search for {@code ID} attribute
   * @return {@linkplain Method} or {@linkplain Field} which represents {@code ID} attribute
   * @throws IllegalArgumentException if {@code ID} attribute was not found
   * @throws NullPointerException if {@code type} argument is null
   */
  Member resolve(Class<?> type);

  /**
   * Same resolver but as a predicate returning {@code true / false} depending
   * if {@code member} is an {@code ID} attribute.
   */
  default Predicate<Member> asPredicate() {
    return other -> {
      Objects.requireNonNull(other, "other");
      try {
        Member member = resolve(other.getDeclaringClass());
        return member.equals(other);
      } catch (IllegalArgumentException e) {
        return false;
      }
    };
  }

  static IdResolver defaultResolver() {
    return fromAnnotation(Criteria.Id.class);
  }

  /**
   * Find {@code ID} attribute marked with a {@code annotation}
   */
  static IdResolver fromAnnotation(Class<? extends Annotation> annotation) {
    Objects.requireNonNull(annotation, "annotation");
    return fromPredicate(a -> a instanceof AnnotatedElement && ((AnnotatedElement) a).isAnnotationPresent(annotation));
  }

  /**
   * Find {@code ID} attribute using a predicate.
   */
  static IdResolver fromPredicate(Predicate<? super Member> predicate) {
    IdResolver resolver = type -> Reflections.findMember(type, predicate)
            .orElseThrow(() -> new IllegalArgumentException(String.format("Member not found in %s using a predicate", type)));
    return CachedIdResolver.of(resolver);
  }

}
