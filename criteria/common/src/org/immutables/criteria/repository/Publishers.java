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

package org.immutables.criteria.repository;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Set of utilities to convert {@link Publisher} into {@link CompletionStage}, {@code List}, other types
 * or add some blocking operations.
 *
 * <p>Currently this class uses rxjava. This is the only place where rxjava is used in {@code common}
 * module. One needs to create rxjava independent converters at some point.
 * TODO:  remove rxjava2 depedency
 */
public final class Publishers {

  private Publishers() {}

  public static <X> CompletionStage<List<X>> toListFuture(Publisher<X> publisher) {
    Objects.requireNonNull(publisher, "publisher");
    return Flowable.fromPublisher(publisher).toList().to(singleToFuture());
  }

  /**
   * Get single result from publisher (blocking operation)
   */
  public static <X> X blockingGet(Publisher<X> publisher) {
    return Flowable.fromPublisher(publisher).singleOrError().blockingGet();
  }

  public static <X> Publisher<X> limit(Publisher<X> publisher, long count) {
    return Flowable.fromPublisher(publisher).limit(count);
  }

  public static <X> Publisher<List<X>> toList(Publisher<X> publisher) {
    return Flowable.fromPublisher(publisher).toList().toFlowable();
  }

  /**
   * Applies map function to each element emitted by {@code publisher}
   */
  public static <T, R> Publisher<R> map(Publisher<T> publisher, Function<? super T, ? extends R> mapper) {
    return Flowable.fromPublisher(publisher).map(mapper::apply);
  }

  public static  <T, R> Publisher<R> flatMapIterable(Publisher<T> publisher, Function<? super T, ? extends Iterable<? extends R>> mapper) {
    return Flowable.fromPublisher(publisher).flatMapIterable(mapper::apply);
  }

  /**
   * Blocking get as list of results
   */
  public static <X> List<X> blockingListGet(Publisher<X> publisher) {
    return Flowable.fromPublisher(publisher).toList().blockingGet();
  }

  private static <X> io.reactivex.functions.Function<Single<X>, CompletionStage<X>> singleToFuture() {
    return single -> {
      final CompletableFuture<X> fut = new CompletableFuture<>();
      Disposable disposable = single.subscribe(fut::complete, fut::completeExceptionally);
      return fut;
    };
  }

  /**
   * Converts a publisher (with a single result) to {@link CompletionStage}.
   */
  public static <X> CompletionStage<X> toFuture(Publisher<X> publisher) {
    Objects.requireNonNull(publisher, "publisher");
    return Flowable.fromPublisher(publisher).singleOrError().to(singleToFuture());
  }
}
