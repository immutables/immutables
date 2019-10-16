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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.matcher.Projection;
import org.immutables.criteria.repository.AbstractReader;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Reactive implementation of the reader.
 */
public final class ReactiveReader<T> extends AbstractReader<ReactiveReader<T>> implements ReactiveFetcher<T> {

  private final Backend.Session session;
  private final Query query;
  private final ReactiveFetcher<T> fetcher;

  public ReactiveReader(Query query, Backend.Session session) {
    super(query);
    this.session = Objects.requireNonNull(session, "session");
    this.query = Objects.requireNonNull(query, "query");
    this.fetcher = ReactiveFetcher.of(query, session);
  }

  @Override
  protected ReactiveReader<T> newReader(Query query) {
    return new ReactiveReader<>(query, session);
  }

  public <T1> ReactiveMapper1<T1> select(Projection<T1> proj1) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1));
    return new ReactiveMapper1<T1>(newQuery, session);
  }

  public <T1, T2> ReactiveMapper2<T1, T2> select(Projection<T1> proj1, Projection<T2> proj2) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2));
    return new ReactiveMapper2<>(newQuery, session);
  }

  public <T1, T2, T3> ReactiveMapper3<T1, T2, T3> select(Projection<T1> proj1, Projection<T2> proj2, Projection<T3> proj3) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2), Matchers.toExpression(proj3));
    return new ReactiveMapper3<>(newQuery, session);
  }

  public <T1, T2, T3, T4> ReactiveMapper4<T1, T2, T3, T4> select(Projection<T1> proj1, Projection<T2> proj2, Projection<T3> proj3, Projection<T4> proj4) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2), Matchers.toExpression(proj3), Matchers.toExpression(proj4));
    return new ReactiveMapper4<>(newQuery, session);
  }

  public <T1, T2, T3, T4, T5> ReactiveMapper5<T1, T2, T3, T4, T5> select(Projection<T1> proj1, Projection<T2> proj2, Projection<T3> proj3, Projection<T4> proj4, Projection<T5> proj5) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2), Matchers.toExpression(proj3), Matchers.toExpression(proj4), Matchers.toExpression(proj5));
    return new ReactiveMapper5<>(newQuery, session);
  }

  public ReactiveMapperTuple select(Iterable<Projection<?>> projections) {
    Objects.requireNonNull(projections, "projections");
    Preconditions.checkArgument(!Iterables.isEmpty(projections), "empty projections");
    List<Expression> expressions = StreamSupport.stream(projections.spliterator(), false).map(Matchers::toExpression).collect(Collectors.toList());
    Query newQuery = this.query.addProjections(expressions);
    return new ReactiveMapperTuple(newQuery, session);
  }

  @Override
  public Publisher<T> fetch() {
    return fetcher.fetch();
  }

  @Override
  public Publisher<T> one() {
    return fetcher.one();
  }

  @Override
  public Publisher<T> oneOrNone() {
    return fetcher.oneOrNone();
  }

  @Override
  public Publisher<Boolean> exists() {
    return fetcher.exists();
  }

  @Override
  public Publisher<Long> count() {
    return fetcher.count();
  }

  @Override
  public <X> ReactiveFetcher<X> map(Function<? super T, ? extends X> mapFn) {
    return fetcher.map(mapFn);
  }

  @Override
  public ReactiveFetcher<T> changeQuery(UnaryOperator<Query> mapFn) {
    return fetcher.changeQuery(mapFn);
  }

}
