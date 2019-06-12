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

package org.immutables.criteria.internal.reactive;

import org.reactivestreams.Publisher;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Utils for <a href="https://www.reactive-streams.org/">reactive streams</a> with typical operators
 * like map / error / flatMap / flatMapIterable which are applied on a {@link Publisher}.
 *
 * <p>This is internal class and should not be used outside criteria and adapters.
 *
 * <p>Many implementations of {@link Publisher} have been copied from
 * <a href="https://github.com/ReactiveX/RxJava">rxjava</a> project.
 *
 * <p>Wherever Criteria should depend directly on existing reactive libraries
 * like <a href="https://github.com/ReactiveX/RxJava">rxjava</a> or <a href="https://projectreactor.io/">reactor</a>
 * instead of home-made / half-copied implementation is a valid question and will be addressed later.
 */
public final class Reactive {

  private Reactive() {}

  /**
   *  Converts streaming elements into a different type using provided {@code mapper}
   */
  public static <T, U> Publisher<U> map(Publisher<T> source, Function<? super T, ? extends U> mapper) {
    return new FlowableMap<>(source, mapper);
  }

  public static <T> Publisher<T> error(Callable<? extends Throwable> callable) {
    return new FlowableError<>(callable);
  }

  /**
   * Return an empty publisher
   */
  @SuppressWarnings("unchecked")
  public static <T> Publisher<T> empty() {
    return (Publisher<T>) FlowableEmpty.INSTANCE;
  }

  public static <T> Publisher<T> just(T value) {
    return new FlowableJust<>(value);
  }

  public static <T> Publisher<T> fromIterable(Iterable<T> iterable) {
    return new FlowableFromIterable<>(iterable);
  }

  /**
   * Used to convert {@code Publisher<Iterable<U>>} into {@code Publisher<U>}
   */
  public static  <T, U> Publisher<U> flatMapIterable(Publisher<T> publisher, final Function<? super T, ? extends Iterable<? extends U>> mapper) {
    return new FlowableFlattenIterable<>(publisher, mapper, 128);
  }

  public static <T> Publisher<T> error(Throwable e) {
    return error(() -> e);
  }

}
