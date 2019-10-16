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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.ImmutableQuery;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.matcher.Projection;
import org.immutables.criteria.repository.AbstractReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Reader returning <a href="https://projectreactor.io">Reactor</a> types
 */
public class ReactorReader<T> extends AbstractReader<ReactorReader<T>> implements ReactorFetcher.LimitOffset<T> {

  private final ImmutableQuery query;
  private final Backend.Session session;
  private final ReactorFetcher<T> fetcher;

  ReactorReader(Query query, Backend.Session session) {
    super(query);
    this.query = ImmutableQuery.copyOf(query);
    this.session = session;
    this.fetcher = ReactorFetcherDelegate.of(query, session);
  }

  @Override
  protected ReactorReader<T> newReader(Query query) {
    return new ReactorReader<>(query, session);
  }


  public <T1> ReactorMapper1.DistinctLimitOffset<T1> select(Projection<T1> proj1) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1));
    return new ReactorMappers.Mapper1<>(newQuery, session);
  }

  public <T1, T2> ReactorMapper2.DistinctLimitOffset<T1, T2> select(Projection<T1> proj1, Projection<T2> proj2) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2));
    return new ReactorMappers.Mapper2<>(newQuery, session);
  }

  public <T1, T2, T3> ReactorMapper3.DistinctLimitOffset<T1, T2, T3> select(Projection<T1> proj1, Projection<T2> proj2, Projection<T3> proj3) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2), Matchers.toExpression(proj3));
    return new ReactorMappers.Mapper3<>(newQuery, session);
  }

  public <T1, T2, T3, T4> ReactorMapper4.DistinctLimitOffset<T1, T2, T3, T4> select(Projection<T1> proj1, Projection<T2> proj2, Projection<T3> proj3, Projection<T4> proj4) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2), Matchers.toExpression(proj3), Matchers.toExpression(proj4));
    return new ReactorMappers.Mapper4<>(newQuery, session);
  }

  public <T1, T2, T3, T4, T5> ReactorMapper5.DistinctLimitOffset<T1, T2, T3, T4, T5> select(Projection<T1> proj1, Projection<T2> proj2, Projection<T3> proj3, Projection<T4> proj4, Projection<T5> proj5) {
    Query newQuery = this.query.addProjections(Matchers.toExpression(proj1), Matchers.toExpression(proj2), Matchers.toExpression(proj3), Matchers.toExpression(proj4), Matchers.toExpression(proj5));
    return new ReactorMappers.Mapper5<>(newQuery, session);
  }

  public ReactorMapperTuple.DistinctLimitOffset select(Iterable<Projection<?>> projections) {
    Objects.requireNonNull(projections, "projections");
    Preconditions.checkArgument(!Iterables.isEmpty(projections), "empty projections");
    List<Expression> expressions = StreamSupport.stream(projections.spliterator(), false).map(Matchers::toExpression).collect(Collectors.toList());
    Query newQuery = this.query.addProjections(expressions);
    return new ReactorMappers.MapperTuple(newQuery, session);
  }


  /**
   * Fetch available results in async fashion
   */
  @Override
  public Flux<T> fetch() {
    return fetcher.fetch();
  }

  @Override
  public Mono<T> one() {
    return fetcher.one();
  }

  @Override
  public Mono<T> oneOrNone() {
    return fetcher.oneOrNone();
  }

  @Override
  public Mono<Boolean> exists() {
    return fetcher.exists();
  }

  @Override
  public Mono<Long> count() {
    return fetcher.count();
  }

  @Override
  public Offset<T> limit(long limit) {
    return newReader(query.withLimit(limit));
  }

  @Override
  public ReactorFetcher<T> offset(long offset) {
    return newReader(query.withOffset(offset));
  }
}
