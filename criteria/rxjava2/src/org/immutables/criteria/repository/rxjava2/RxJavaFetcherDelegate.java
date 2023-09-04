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

package org.immutables.criteria.repository.rxjava2;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.ImmutableQuery;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.reactive.ReactiveFetcher;

import java.util.Objects;
import java.util.function.UnaryOperator;

class RxJavaFetcherDelegate<T> implements RxJavaFetcher<T>, RxJavaFetcher.DistinctLimitOffset<T> {

  private final ReactiveFetcher<T> delegate;

  private RxJavaFetcherDelegate(ReactiveFetcher<T> delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public Flowable<T> fetch() {
    return Flowable.fromPublisher(delegate.fetch());
  }

  @Override
  public Single<T> one() {
    return Single.fromPublisher(delegate.one());
  }

  @Override
  public Maybe<T> oneOrNone() {
    return Flowable.fromPublisher(delegate.oneOrNone()).singleElement();
  }

  @Override
  public Single<Boolean> exists() {
    return Single.fromPublisher(delegate.exists());
  }

  @Override
  public Single<Long> count() {
    return Single.fromPublisher(delegate.count());
  }

  static <T> RxJavaFetcherDelegate<T> fromReactive(ReactiveFetcher<T> delegate) {
    return new RxJavaFetcherDelegate<T>(delegate);
  }

  static <T> RxJavaFetcherDelegate<T> of(Query query, Backend.Session session) {
    return fromReactive(ReactiveFetcher.of(query, session));
  }

  private RxJavaFetcherDelegate<T> changeQuery(UnaryOperator<Query> fn) {
    return new RxJavaFetcherDelegate<>(delegate.changeQuery(fn));
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
  public RxJavaFetcher<T> offset(long offset) {
    return changeQuery(query -> ImmutableQuery.copyOf(query).withOffset(offset));
  }
}
