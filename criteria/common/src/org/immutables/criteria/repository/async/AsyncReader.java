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
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.matcher.Projection;
import org.immutables.criteria.repository.AbstractReader;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class AsyncReader<T> extends AbstractReader<AsyncReader<T>> implements AsyncFetcher<T> {

  private final Query query;
  private final Backend.Session session;
  private final AsyncFetcherDelegate<T> fetcher;

  AsyncReader(Query query, Backend.Session session) {
    super(query);
    this.query = query;
    this.session = Objects.requireNonNull(session, "session");
    this.fetcher = AsyncFetcherDelegate.of(query, session);
  }

  @Override
  protected AsyncReader<T> newReader(Query query) {
    return new AsyncReader<>(query, session);
  }

  public <T1> AsyncMapper1<T1> select(Projection<T1> proj1) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1));
    return new AsyncMapper1<T1>(newQuery, session);
  }

  public <T1, T2> AsyncMapper2<T1, T2> select(Projection<T1> proj1, Projection<T2> proj2) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2));
    return new AsyncMapper2<>(newQuery, session);
  }

  public <T1, T2, T3> AsyncMapper3<T1, T2, T3> select(Projection<T1> proj1, Projection<T2> proj2, Projection<T3> proj3) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2), Matchers.toExpression(proj3));
    return new AsyncMapper3<>(newQuery, session);
  }

  public <T1, T2, T3, T4> AsyncMapper4<T1, T2, T3, T4> select(Projection<T1> proj1, Projection<T2> proj2, Projection<T3> proj3, Projection<T4> proj4) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2), Matchers.toExpression(proj3), Matchers.toExpression(proj4));
    return new AsyncMapper4<>(newQuery, session);
  }

  public <T1, T2, T3, T4, T5> AsyncMapper5<T1, T2, T3, T4, T5> select(Projection<T1> proj1, Projection<T2> proj2, Projection<T3> proj3, Projection<T4> proj4, Projection<T5> proj5) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2), Matchers.toExpression(proj3), Matchers.toExpression(proj4), Matchers.toExpression(proj5));
    return new AsyncMapper5<>(newQuery, session);
  }

  @Override
  public CompletionStage<List<T>> fetch() {
    return fetcher.fetch();
  }

  @Override
  public CompletionStage<T> one() {
    return fetcher.one();
  }

  @Override
  public CompletionStage<Optional<T>> oneOrNone() {
    return fetcher.oneOrNone();
  }

  @Override
  public CompletionStage<Boolean> exists() {
    return fetcher.exists();
  }
}
