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

package org.immutables.criteria.repository.rxjava;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.reactive.ReactiveFetcher;

import java.util.Objects;

class RxJavaFetcherDelegate<T> implements RxJavaFetcher<T> {

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

  static <T> RxJavaFetcherDelegate<T> fromReactive(ReactiveFetcher<T> delegate) {
    return new RxJavaFetcherDelegate<T>(delegate);
  }

  static <T> RxJavaFetcherDelegate<T> of(Query query, Backend.Session session) {
    return fromReactive(ReactiveFetcher.of(query, session));
  }
}
