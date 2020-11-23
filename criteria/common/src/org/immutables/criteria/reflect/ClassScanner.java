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

package org.immutables.criteria.reflect;

import org.immutables.value.Value;

import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Iterates through class hierarchy using reflection api looking for methods, fields
 * and constructors.
 *
 * <p>This class is not thread-safe</p>
 */
public interface ClassScanner extends Iterable<Member> {

  default Stream<Member> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  /**
   * Return cached version of the scanner. Reflection API is used only once.
   * new scanner is thread-safe
   */
  default ClassScanner cache() {
    return PrecachedClassScanner.of(this);
  }

  static ClassScanner of(Class<?> type) {
    return builder(type).build();
  }

  static ClassScanner onlyFields(Class<?> type) {
    return builder(type).withMethods(false).withFields(true).build();
  }

  static ClassScanner onlyMethods(Class<?> type) {
    return builder(type).withMethods(true).withFields(false).build();
  }

  static ClassScanner concat(ClassScanner scan1, ClassScanner scan2) {
    return new ConcatScanner(Arrays.asList(scan1, scan2));
  }

  static ImmutableBuilder builder(Class<?> type) {
    return ImmutableBuilder.of(type);
  }

  @Value.Immutable
  interface Builder {

    @Value.Parameter
    Class<?> type();

    /**
     * Include fields
     */
    @Value.Default
    default boolean fields() {
      return true;
    }

    /**
     * Include methods
     */
    @Value.Default
    default boolean methods() {
      return true;
    }

    /**
     * Include constructors
     */
    @Value.Default
    default boolean constructors() {
      return false;
    }

    /**
     * Scan superclass hierarchy
     */
    @Value.Default
    default boolean superclass() {
      return true;
    }

    @Value.Default
    default boolean interfaces() {
      return true;
    }

    default ClassScanner build() {
      return new LazyClassScanner(ImmutableBuilder.copyOf(this));
    }
  }
}
