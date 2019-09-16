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

package org.immutables.check;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Similar check to {@link IterableChecker} but on streams.
 *
 * Currently resides in criteria-common because {@code testing} module is still oon Java 7.
 */
public class StreamChecker<Z extends Stream<T>, T> {

  private final Stream<T> stream;
  private IterableChecker<List<T>, T> delegate; // lazy

  private StreamChecker(Stream<T> stream) {
    this.stream = Objects.requireNonNull(stream, "stream");
  }

  /**
   * Lazily instantiate {@link IterableChecker}
   */
  private IterableChecker<List<T>, T> lazyGet() {
    IterableChecker<List<T>, T> delegate = this.delegate;
    if (delegate == null) {
      List<T> asList = stream.collect(Collectors.toList());
      delegate = new IterableChecker<>(stream.collect(Collectors.toList()), false);
      this.delegate = delegate;
    }

    return delegate;
  }

  public void has(T element) {
    lazyGet().has(element);
  }

  @SafeVarargs
  public final void hasAll(T... elements) {
    lazyGet().hasAll(elements);
  }

  public final void hasAll(Iterable<? extends T> elements) {
    lazyGet().hasAll(elements);
  }

  public void isOf(Iterable<?> elements) {
    lazyGet().isOf(elements);
  }

  @SafeVarargs
  public final void isOf(T... elements) {
    lazyGet().isOf(elements);
  }

  public void hasContentInAnyOrder(Iterable<? extends T> elements) {
    lazyGet().hasContentInAnyOrder(elements);
  }

  @SafeVarargs
  public final void hasContentInAnyOrder(T... elements) {
    lazyGet().hasContentInAnyOrder(elements);
  }

  public void hasSize(int size) {
    lazyGet().hasSize(size);
  }

  public void notEmpty() {
    lazyGet().notEmpty();
  }

  public void isEmpty() {
    lazyGet().isEmpty();
  }

  public static <T> StreamChecker<Stream<T>, T> of(Stream<T> stream) {
    return new StreamChecker<>(stream);
  }

}
