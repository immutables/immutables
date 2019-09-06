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
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.Fetcher;
import org.immutables.criteria.repository.Publishers;
import org.reactivestreams.Publisher;

import java.util.Objects;
import java.util.function.Function;

/**
 * Reactive implementation for fetcher
 */
public class ReactiveFetcher<T> implements Fetcher<Publisher<T>> {

  private final Query query;
  private final Backend.Session session;

  public ReactiveFetcher(Query query, Backend.Session session) {
    this.query = Objects.requireNonNull(query, "query");
    this.session = Objects.requireNonNull(session, "session");
  }

  @Override
  public Publisher<T> fetch() {
    return session.execute(StandardOperations.Select.of(query)).publisher();
  }

  public <X> ReactiveFetcher<X> map(Function<T, X> mapFn) {
    return new MappedFetcher<T, X>(query, session, mapFn);
  }

  private static class MappedFetcher<T, R> extends ReactiveFetcher<R> {
    private final Function<? super T, ? extends R> mapFn;

    private MappedFetcher(Query query, Backend.Session session, Function<? super T, ? extends R> mapFn) {
      super(query, session);
      this.mapFn = Objects.requireNonNull(mapFn, "mapFn");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Publisher<R> fetch() {
      return Publishers.map((Publisher<T>) super.fetch(), mapFn);
    }
  }


}
