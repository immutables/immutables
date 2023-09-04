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

package org.immutables.criteria.reactor;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.ImmutableQuery;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.reactive.ReactiveFetcher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.UnaryOperator;

class ReactorFetcherDelegate<T> implements ReactorFetcher<T>, ReactorFetcher.DistinctLimitOffset<T> {

  private final ReactiveFetcher<T> delegate;

  private ReactorFetcherDelegate(ReactiveFetcher<T> delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  private ReactorFetcherDelegate<T> changeQuery(UnaryOperator<Query> fn) {
    return new ReactorFetcherDelegate<>(delegate.changeQuery(fn));
  }


  @Override
  public LimitOffset<T> distinct() {
    return changeQuery(query -> ImmutableQuery.copyOf(query).withDistinct(true));
  }

  @Override
  public Offset<T> limit(long limit) {
    return changeQuery(query -> ImmutableQuery.copyOf(query).withLimit(limit));
  }

  @Override
  public ReactorFetcherDelegate<T> offset(long offset) {
    return changeQuery(query -> ImmutableQuery.copyOf(query).withOffset(offset));
  }

  @Override
  public Flux<T> fetch() {
    return Flux.from(delegate.fetch());
  }

  @Override
  public Mono<T> one() {
    return Mono.from(delegate.one());
  }

  @Override
  public Mono<T> oneOrNone() {
    return Mono.from(delegate.oneOrNone());
  }

  @Override
  public Mono<Boolean> exists() {
    return Mono.from(delegate.exists());
  }

  @Override
  public Mono<Long> count() {
    return Mono.from(delegate.count());
  }

  static <T> ReactorFetcherDelegate<T> fromReactive(ReactiveFetcher<T> delegate) {
    return new ReactorFetcherDelegate<T>(delegate);
  }

  static <T> ReactorFetcherDelegate<T> of(Query query, Backend.Session session) {
    return fromReactive(ReactiveFetcher.of(query, session));
  }
}
