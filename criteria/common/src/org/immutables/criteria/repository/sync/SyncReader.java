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

package org.immutables.criteria.repository.sync;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.matcher.Projection;
import org.immutables.criteria.repository.AbstractReader;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Synchronous (blocking) reader operations.
 * @param <T> entity type
 */
public class SyncReader<T> extends AbstractReader<SyncReader<T>> implements SyncFetcher<T> {

  private final Query query;
  private final Backend.Session session;
  private final SyncFetcher<T> fetcher;

  public SyncReader(Query query, Backend.Session session) {
    super(query);
    this.query = query;
    this.session = session;
    this.fetcher = SyncFetcherDelegate.of(query, session);
  }

  @Override
  protected SyncReader<T> newReader(Query query) {
    return new SyncReader<>(query, session);
  }

  public <T1> SyncMapper1<T1> select(Projection<T1> proj1) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1));
    return new SyncMapper1<T1>(newQuery, session);
  }

  public <T1, T2> SyncMapper2<T1, T2> select(Projection<T1> proj1, Projection<T2> proj2) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2));
    return new SyncMapper2<>(newQuery, session);
  }

  public <T1, T2, T3> SyncMapper3<T1, T2, T3> select(Projection<T1> proj1, Projection<T2> proj2, Projection<T3> proj3) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2), Matchers.toExpression(proj3));
    return new SyncMapper3<>(newQuery, session);
  }

  public <T1, T2, T3, T4> SyncMapper4<T1, T2, T3, T4> select(Projection<T1> proj1, Projection<T2> proj2, Projection<T3> proj3, Projection<T4> proj4) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2), Matchers.toExpression(proj3), Matchers.toExpression(proj4));
    return new SyncMapper4<>(newQuery, session);
  }

  public <T1, T2, T3, T4, T5> SyncMapper5<T1, T2, T3, T4, T5> select(Projection<T1> proj1, Projection<T2> proj2, Projection<T3> proj3, Projection<T4> proj4, Projection<T5> proj5) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2), Matchers.toExpression(proj3), Matchers.toExpression(proj4), Matchers.toExpression(proj5));
    return new SyncMapper5<>(newQuery, session);
  }

  public SyncMapperTuple select(Iterable<Projection<?>> projections) {
    Objects.requireNonNull(projections, "projections");
    Preconditions.checkArgument(!Iterables.isEmpty(projections), "empty projections");
    List<Expression> expressions = StreamSupport.stream(projections.spliterator(), false).map(Matchers::toExpression).collect(Collectors.toList());
    Query newQuery = this.query.addProjections(expressions);
    return new SyncMapperTuple(newQuery, session);
  }

  @Override
  public List<T> fetch() {
    return fetcher.fetch();
  }

  @Override
  public T one() {
    return fetcher.one();
  }

  @Override
  public Optional<T> oneOrNone() {
    return fetcher.oneOrNone();
  }

  @Override
  public boolean exists() {
    return fetcher.exists();
  }

  @Override
  public long count() {
    return fetcher.count();
  }

}
