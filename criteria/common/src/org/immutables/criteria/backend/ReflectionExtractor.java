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

package org.immutables.criteria.backend;

import com.google.common.base.Preconditions;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

final class ReflectionExtractor {

  public static <T, K> Function<T, K> of(Class<T> type, Predicate<AnnotatedElement> predicate) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(predicate, "predicate");

    final Set<Class<?>> visited = new HashSet<>();
    visited.add(Object.class); // don't visit Object
    final Deque<Class<?>> toVisit = new ArrayDeque<>();
    toVisit.push(type);
    while (!toVisit.isEmpty()) {
      final Class<?> current = toVisit.pop();
      if (!visited.add(current)) {
        continue;
      }

      // look for fields
      for (Field field: current.getFields()) {
        if (predicate.test(field)) {
          return new FieldExtractor<>(field);
        }
      }

      // look for methods
      for (Method method: current.getMethods()) {
        if (method.getParameterCount() == 0 &&
                Modifier.isPublic(method.getModifiers()) &&
                predicate.test(method)) {
          return new MethodExtractor<>(method);
        }
      }

      if (!current.isInterface()) {
        toVisit.push(current.getSuperclass());
      }
      toVisit.addAll(Arrays.asList(current.getInterfaces()));
    }

    throw new IllegalArgumentException(String.format("None of the fields or methods from %s matched predicate", type));
  }

  public static <T, K> Function<T, K> of(Class<T> type, Class<? extends Annotation> annotation) {
    Objects.requireNonNull(annotation, "annotation");
    try {
      return of(type, elem -> elem.isAnnotationPresent(annotation));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(String.format("Annotation %s not found in %s", annotation, type));
    }
  }


  /**
   * Extracts value by calling a method using reflection
   */
  private static class MethodExtractor<T, K> implements Function<T, K> {
    private final Method method;

    private MethodExtractor(Method method) {
      this.method = Objects.requireNonNull(method, "method");
      if (!method.isAccessible()) {
        method.setAccessible(true);
      }
      Preconditions.checkArgument(method.getParameterCount() == 0, "expected not parameters for %s", method);
    }

    @Override
    public K apply(T instance) {
      Objects.requireNonNull(instance, "instance");
      try {
        @SuppressWarnings("unchecked")
        K result = (K) method.invoke(instance);
        return result;
      } catch (IllegalAccessException| InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Used to extract value from a field
   */
  private static class FieldExtractor<T, K> implements Function<T, K> {
    private final Field field;

    private FieldExtractor(Field field) {
      this.field = Objects.requireNonNull(field, "field");
      if (!field.isAccessible()) {
        field.setAccessible(true);
      }
    }

    @Override
    public K apply(T instance) {
      Objects.requireNonNull(instance, "instance");
      try {
        @SuppressWarnings("unchecked")
        final K result = (K) field.get(instance);
        return result;
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private ReflectionExtractor() {}
}
