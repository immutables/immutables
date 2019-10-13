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

import com.google.common.base.Preconditions;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Reflection utilities
 */
public final class Reflections {

  public static Optional<Member> findMember(Class<?> type, Predicate<? super Member> predicate) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(predicate, "predicate");
    return ClassScanner.of(type)
            .stream()
            .filter(predicate)
            .findAny();
  }

  public static Function<Object, Object> extractorFor(Member member) {
    Objects.requireNonNull(member, "member");
    if (member instanceof Field) {
      return new FieldExtractor((Field) member);
    } else if (member instanceof Method) {
      return new MethodExtractor((Method) member);
    }

    throw new IllegalArgumentException("Member is not a field nor a method: " + member);
  }


  /**
   * Extracts value by calling a method using reflection
   */
  private static class MethodExtractor implements Function<Object, Object> {
    private final Method method;

    private MethodExtractor(Method method) {
      this.method = Objects.requireNonNull(method, "method");
      if (!method.isAccessible()) {
        method.setAccessible(true);
      }
      Preconditions.checkArgument(method.getParameterCount() == 0, "expected not parameters for %s", method);
    }

    @Override
    public Object apply(Object instance) {
      Objects.requireNonNull(instance, "instance");
      try {
        return method.invoke(instance);
      } catch (IllegalAccessException| InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Used to extract value from a field
   */
  private static class FieldExtractor implements Function<Object, Object> {
    private final Field field;

    private FieldExtractor(Field field) {
      this.field = Objects.requireNonNull(field, "field");
      if (!field.isAccessible()) {
        field.setAccessible(true);
      }
    }

    @Override
    public Object apply(Object instance) {
      Objects.requireNonNull(instance, "instance");
      try {
        return field.get(instance);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private Reflections() {}
}
