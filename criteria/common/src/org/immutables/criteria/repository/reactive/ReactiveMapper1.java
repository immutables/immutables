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

package org.immutables.criteria.repository.reactive;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.Mappers;
import org.immutables.criteria.repository.Tuple;
import org.reactivestreams.Publisher;

import java.util.Optional;
import java.util.function.Function;

public class ReactiveMapper1<T1> implements ReactiveFetcher<T1> {

  private final Query query;
  private final Backend.Session session;
  private final ReactiveFetcher<T1> fetcher;

  public ReactiveMapper1(Query query, Backend.Session session) {
    this.query = query;
    this.session = session;
    this.fetcher = ReactiveFetcher.<Tuple>of(query, session).map(Mappers.<T1>fromTuple());
  }

  public ReactiveFetcher<Optional<T1>> asOptional() {
    return ReactiveFetcher.<Tuple>of(query, session).map(Mappers.<T1>fromTuple().andThen(Optional::ofNullable));
  }

  @Override
  public Publisher<T1> fetch() {
    return fetcher.fetch();
  }

  @Override
  public Publisher<T1> one() {
    return fetcher.one();
  }

  @Override
  public Publisher<T1> oneOrNone() {
    return fetcher.oneOrNone();
  }

  @Override
  public Publisher<Boolean> exists() {
    return fetcher.exists();
  }

  @Override
  public <X> ReactiveFetcher<X> map(Function<? super T1, ? extends X> mapFn) {
    return fetcher.map(mapFn);
  }

}
