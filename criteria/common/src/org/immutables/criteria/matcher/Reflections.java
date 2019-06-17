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

package org.immutables.criteria.matcher;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * Reflection utils
 */
final class Reflections {

  private Reflections() {}

  public static Member member(Class<?> clazz, String name) {
    return findMember(clazz, name)
            .orElseThrow(() -> new IllegalArgumentException(String.format("Member %s not found in %s", name, clazz)));
  }

  public static Optional<Member> findMember(Class<?> clazz, String name) {
    final Optional<Method> method = findMethod(clazz, name);
    if (method.isPresent()) {
      return method.map(m -> m);
    }

    return findField(clazz, name).map(f -> f);
  }

  /**
   * Find a field in a particular class
   */
  public static Optional<Field> findField(Class<?> clazz, String name) {
    Objects.requireNonNull(clazz, "clazz");
    Objects.requireNonNull(name, "name");

    while (clazz != null && !clazz.equals(Object.class)) {
      try {
        return Optional.of(clazz.getDeclaredField(name));
      } catch (SecurityException | NoSuchFieldException e) {
        // skip
      }
      clazz = clazz.getSuperclass();
    }


    return Optional.empty();

  }

  /**
   * Find a method in a particular class
   */
  public static Optional<Method> findMethod(Class<?> clazz, String name) {
    Objects.requireNonNull(clazz, "clazz");
    Objects.requireNonNull(name, "name");

    while (clazz != null && !clazz.equals(Object.class)) {
      try {
        return Optional.of(clazz.getDeclaredMethod(name));
      } catch (SecurityException | NoSuchMethodException e) {
        // skip
      }
      clazz = clazz.getSuperclass();
    }

    return Optional.empty();
  }

}
