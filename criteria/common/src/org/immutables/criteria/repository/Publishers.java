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
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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

  /**
   * Blocking get as list of results
   */
  public static <X> List<X> blockingListGet(Publisher<X> publisher) {
    return Flowable.fromPublisher(publisher).toList().blockingGet();
  }

  private static <X> io.reactivex.functions.Function<Single<X>, CompletionStage<X>> singleToFuture() {
    return single -> {
      final CompletableFuture<X> fut = new CompletableFuture<>();
      single.subscribe(fut::complete, fut::completeExceptionally);
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
