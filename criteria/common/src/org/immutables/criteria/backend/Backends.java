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
import org.immutables.criteria.Criteria;

import java.lang.annotation.Annotation;
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

/**
 * Utils for backend implementations
 */
public final class Backends {

  private Backends() {}

  /**
   * Return function which extracts identifier (key) from existing instance.
   * Identifier is defined on POJOs with {@link Criteria.Id} annotation.
   * @throws IllegalArgumentException if {@link Criteria.Id} annotation is not declared in any methods
   * or fields.
   */
  public static <T, K> Function<T, K> idExtractor(final Class<T> type) {
    Objects.requireNonNull(type, "type");
    final Class<? extends Annotation> annotationClass = Criteria.Id.class;
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
        if (field.getAnnotation(annotationClass) != null) {
          if (!field.isAccessible()) {
            field.setAccessible(true);
          }
          return new FieldExtractor<>(field);
        }
      }

      // look for methods
      for (Method method: current.getMethods()) {
        if (method.getParameterCount() == 0 &&
                Modifier.isPublic(method.getModifiers()) &&
                method.getAnnotation(annotationClass) != null) {
          if (!method.isAccessible()) {
            method.setAccessible(true);
          }
          return new MethodExtractor<>(method);
        }
      }

      if (!current.isInterface()) {
        toVisit.push(current.getSuperclass());
      }
      toVisit.addAll(Arrays.asList(current.getInterfaces()));
    }

    throw new IllegalArgumentException(String.format("Annotation %s not found in methods or fields of %s", annotationClass.getName(), type));
  }

  /**
   * Extracts value by calling a method using reflection
   */
  private static class MethodExtractor<T, K> implements Function<T, K> {
    private final Method method;

    private MethodExtractor(Method method) {
      this.method = Objects.requireNonNull(method, "method");
      Preconditions.checkArgument(method.getParameterCount() == 0, "expected not parameters for %s", method);
    }

    @Override
    public K apply(T instance) {
      Objects.requireNonNull(instance, "instance");
      try {
        @SuppressWarnings("unchecked")
        K result = (K) method.invoke(instance);
        return result;
      } catch (IllegalAccessException|InvocationTargetException e) {
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

}
