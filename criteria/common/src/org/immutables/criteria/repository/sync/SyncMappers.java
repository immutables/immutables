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

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.ImmutableQuery;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.MapperFunction3;
import org.immutables.criteria.repository.MapperFunction4;
import org.immutables.criteria.repository.MapperFunction5;
import org.immutables.criteria.repository.Tuple;
import org.immutables.criteria.repository.reactive.ReactiveMapper1;
import org.immutables.criteria.repository.reactive.ReactiveMapper2;
import org.immutables.criteria.repository.reactive.ReactiveMapper3;
import org.immutables.criteria.repository.reactive.ReactiveMapper4;
import org.immutables.criteria.repository.reactive.ReactiveMapper5;
import org.immutables.criteria.repository.reactive.ReactiveMapperTuple;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Default implementation for various synchronous mappers
 */
final class SyncMappers {

  static class Mapper1<T1> implements SyncMapper1<T1>, SyncMapper1.DistinctLimitOffset<T1> {

    private final ReactiveMapper1<T1> mapper;
    private final SyncFetcher.DistinctLimitOffset<T1> fetcher;

    Mapper1(Query query, Backend.Session session) {
      this(new ReactiveMapper1<>(query, session));
    }

    private Mapper1(ReactiveMapper1<T1> mapper) {
      this(mapper, SyncFetcherDelegate.fromReactive(mapper));
    }

    private Mapper1(ReactiveMapper1<T1> mapper, SyncFetcher<T1> fetcher) {
      this.mapper = mapper;
      this.fetcher = (SyncFetcher.DistinctLimitOffset<T1>) fetcher;
    }

    @Override
    public DistinctLimitOffset<Optional<T1>> asOptional() {
      return new Mapper1<>(mapper.asOptional());
    }

    @Override
    public List<T1> fetch() {
      return fetcher.fetch();
    }

    @Override
    public T1 one() {
      return fetcher.one();
    }

    @Override
    public Optional<T1> oneOrNone() {
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

    @Override
    public LimitOffset<T1> distinct() {
      return new Mapper1<>(mapper, fetcher.distinct());
    }

    @Override
    public Offset<T1> limit(long limit) {
      return new Mapper1<>(mapper, fetcher.limit(limit));
    }

    @Override
    public SyncMapper1<T1> offset(long offset) {
      return new Mapper1<>(mapper, fetcher.offset(offset));
    }
  }

  static class Mapper2<T1, T2> implements SyncMapper2<T1, T2>, SyncMapper2.DistinctLimitOffset<T1, T2> {

    private final ImmutableQuery query;
    private final Backend.Session session;

    Mapper2(Query query, Backend.Session session) {
      this.query = ImmutableQuery.copyOf(query);
      this.session = session;
    }

    @Override
    public <R> SyncFetcher<R> map(BiFunction<T1, T2, R> mapFn) {
      ReactiveMapper2<T1, T2> delegate = new ReactiveMapper2<>(query, session);
      return SyncFetcherDelegate.fromReactive(delegate.map(mapFn));
    }

    @Override
    public <R> SyncFetcher<R> map(Function<? super Tuple, ? extends R> mapFn) {
      ReactiveMapper2<T1, T2> delegate = new ReactiveMapper2<>(query, session);
      return SyncFetcherDelegate.fromReactive(delegate.map(mapFn));
    }

    @Override
    public LimitOffset<T1, T2> distinct() {
      return new Mapper2<>(query.withDistinct(true), session);
    }

    @Override
    public Offset<T1, T2> limit(long limit) {
      return new Mapper2<>(query.withLimit(limit), session);
    }

    @Override
    public SyncMapper2<T1, T2> offset(long offset) {
      return new Mapper2<>(query.withOffset(offset), session);
    }
  }

  static class Mapper3<T1, T2, T3> implements SyncMapper3<T1, T2, T3>, SyncMapper3.DistinctLimitOffset<T1, T2, T3> {

    private final ImmutableQuery query;
    private final Backend.Session session;

    Mapper3(Query query, Backend.Session session) {
      this.query = ImmutableQuery.copyOf(query);
      this.session = session;
    }

    @Override
    public <R> SyncFetcher<R> map(MapperFunction3<T1, T2, T3, R> mapFn) {
      ReactiveMapper3<T1, T2, T3> delegate = new ReactiveMapper3<>(query, session);
      return SyncFetcherDelegate.fromReactive(delegate.map(mapFn));
    }

    @Override
    public <R> SyncFetcher<R> map(Function<? super Tuple, ? extends R> mapFn) {
      ReactiveMapper3<T1, T2, T3> delegate = new ReactiveMapper3<>(query, session);
      return SyncFetcherDelegate.fromReactive(delegate.map(mapFn));
    }

    @Override
    public LimitOffset<T1, T2, T3> distinct() {
      return new Mapper3<>(query.withDistinct(true), session);
    }

    @Override
    public Offset<T1, T2, T3> limit(long limit) {
      return new Mapper3<>(query.withLimit(limit), session);
    }

    @Override
    public SyncMapper3<T1, T2, T3> offset(long offset) {
      return new Mapper3<>(query.withOffset(offset), session);
    }
  }

  static class Mapper4<T1, T2, T3, T4> implements SyncMapper4<T1, T2, T3, T4>, SyncMapper4.DistinctLimitOffset<T1, T2, T3, T4> {

    private final ImmutableQuery query;
    private final Backend.Session session;

    Mapper4(Query query, Backend.Session session) {
      this.query = ImmutableQuery.copyOf(query);
      this.session = session;
    }

    @Override
    public <R> SyncFetcher<R> map(MapperFunction4<T1, T2, T3, T4, R> mapFn) {
      ReactiveMapper4<T1, T2, T3, T4> delegate = new ReactiveMapper4<>(query, session);
      return SyncFetcherDelegate.fromReactive(delegate.map(mapFn));
    }

    @Override
    public <R> SyncFetcher<R> map(Function<? super Tuple, ? extends R> mapFn) {
      ReactiveMapper4<T1, T2, T3, T4> delegate = new ReactiveMapper4<>(query, session);
      return SyncFetcherDelegate.fromReactive(delegate.map(mapFn));
    }

    @Override
    public LimitOffset<T1, T2, T3, T4> distinct() {
      return new Mapper4<>(query.withDistinct(true), session);
    }

    @Override
    public Offset<T1, T2, T3, T4> limit(long limit) {
      return new Mapper4<>(query.withLimit(limit), session);
    }

    @Override
    public SyncMapper4<T1, T2, T3, T4> offset(long offset) {
      return new Mapper4<>(query.withOffset(offset), session);
    }
  }

  static class Mapper5<T1, T2, T3, T4, T5> implements SyncMapper5<T1, T2, T3, T4, T5>, SyncMapper5.DistinctLimitOffset<T1, T2, T3, T4, T5> {

    private final ImmutableQuery query;
    private final Backend.Session session;

    Mapper5(Query query, Backend.Session session) {
      this.query = ImmutableQuery.copyOf(query);
      this.session = session;
    }

    @Override
    public <R> SyncFetcher<R> map(MapperFunction5<T1, T2, T3, T4, T5, R> mapFn) {
      ReactiveMapper5<T1, T2, T3, T4, T5> delegate = new ReactiveMapper5<>(query, session);
      return SyncFetcherDelegate.fromReactive(delegate.map(mapFn));
    }

    @Override
    public <R> SyncFetcher<R> map(Function<? super Tuple, ? extends R> mapFn) {
      ReactiveMapper5<T1, T2, T3, T4, T5> delegate = new ReactiveMapper5<>(query, session);
      return SyncFetcherDelegate.fromReactive(delegate.map(mapFn));
    }

    @Override
    public LimitOffset<T1, T2, T3, T4, T5> distinct() {
      return new Mapper5<>(query.withDistinct(true), session);
    }

    @Override
    public Offset<T1, T2, T3, T4, T5> limit(long limit) {
      return new Mapper5<>(query.withLimit(limit), session);
    }

    @Override
    public SyncMapper5<T1, T2, T3, T4, T5> offset(long offset) {
      return new Mapper5<>(query.withOffset(offset), session);
    }
  }

  static class MapperTuple implements SyncMapperTuple, SyncMapperTuple.DistinctLimitOffset {
    private final ImmutableQuery query;
    private final Backend.Session session;

    MapperTuple(Query query, Backend.Session session) {
      this.query = ImmutableQuery.copyOf(query);
      this.session = session;
    }

    @Override
    public <R> SyncFetcher<R> map(Function<? super Tuple, ? extends R> mapFn) {
      ReactiveMapperTuple mapper = new ReactiveMapperTuple(query, session);
      return SyncFetcherDelegate.fromReactive(mapper.map(mapFn));
    }

    @Override
    public LimitOffset distinct() {
      return new MapperTuple(query.withDistinct(true), session);
    }

    @Override
    public Offset limit(long limit) {
      return new MapperTuple(query.withLimit(limit), session);
    }

    @Override
    public SyncMapperTuple offset(long offset) {
      return new MapperTuple(query.withOffset(offset), session);
    }
  }

  private SyncMappers() {}

}
