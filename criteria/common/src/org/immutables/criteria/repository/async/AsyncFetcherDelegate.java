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

package org.immutables.criteria.repository.async;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.Publishers;
import org.immutables.criteria.repository.reactive.ReactiveFetcher;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

class AsyncFetcherDelegate<T> implements AsyncFetcher<T> {

  private final ReactiveFetcher<T> fetcher;

  private AsyncFetcherDelegate(ReactiveFetcher<T> fetcher) {
    this.fetcher = Objects.requireNonNull(fetcher, "fetcher");
  }

  @Override
  public CompletionStage<List<T>> fetch() {
    return Publishers.toListFuture(fetcher.fetch());
  }

  @Override
  public CompletionStage<T> one() {
    return Publishers.toFuture(fetcher.one());
  }

  @Override
  public CompletionStage<Optional<T>> oneOrNone() {
    CompletionStage<List<T>> future = Publishers.toListFuture(fetcher.oneOrNone());
    return future.thenApply(list -> list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.get(0)));
  }

  @Override
  public CompletionStage<Boolean> exists() {
    return Publishers.toFuture(fetcher.exists());
  }

  @Override
  public CompletionStage<Long> count() {
    return Publishers.toFuture(fetcher.count());
  }

  static <T> AsyncFetcherDelegate<T> fromReactive(ReactiveFetcher<T> fetcher) {
    return new AsyncFetcherDelegate<T>(fetcher);
  }

  static <T> AsyncFetcherDelegate<T> of(Query query, Backend.Session session) {
    return new AsyncFetcherDelegate<T>(ReactiveFetcher.of(query, session));
  }



}
