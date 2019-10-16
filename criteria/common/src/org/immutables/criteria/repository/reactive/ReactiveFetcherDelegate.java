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
import org.immutables.criteria.backend.NonUniqueResultException;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.expression.ImmutableQuery;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.matcher.Projection;
import org.immutables.criteria.repository.Publishers;
import org.immutables.criteria.repository.Tuple;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

class ReactiveFetcherDelegate<T> implements ReactiveFetcher<T> {

  private final ImmutableQuery query;
  private final Backend.Session session;

  private ReactiveFetcherDelegate(Query query, Backend.Session session) {
    Objects.requireNonNull(query, "query");
    this.query = ImmutableQuery.copyOf(query);
    this.session = Objects.requireNonNull(session, "session");
  }

  @SuppressWarnings("unchecked")
  static <T> ReactiveFetcher<T> of(Query query, Backend.Session session) {
    return query.hasProjections() ? (ReactiveFetcher<T>) ofTuple(query, session) : new ReactiveFetcherDelegate<>(query, session);
  }

  private static ReactiveFetcher<Tuple> ofTuple(Query query, Backend.Session session) {
    return new ReactiveFetcherDelegate<ProjectedTuple>(query, session).map(TupleAdapter::new);
  }

  @Override
  public Publisher<T> fetch() {
    return session.execute(StandardOperations.Select.of(query)).publisher();
  }

  @Override
  public Publisher<T> one() {
    Consumer<List<T>> checkFn = list -> {
      if (list.size() != 1) {
         throw new NonUniqueResultException(String.format("Expected exactly one element but got %d for query %s", list.size(), query));
      }
    };

    return validateAsList(checkFn);
  }

  @Override
  public Publisher<T> oneOrNone() {
    Consumer<List<T>> checkFn = list -> {
      if (list.size() > 1) {
        throw new NonUniqueResultException(String.format("Expected at most one element but got (at least) %d for query %s", list.size(), query));
      }
    };

    return validateAsList(checkFn);
  }

  private Publisher<T> validateAsList(Consumer<List<T>> validatorFn) {
    ImmutableQuery query = ImmutableQuery.copyOf(this.query);
    if (!query.limit().isPresent()) {
      // ensure at most one element
      // fail if there are 2 or more
      query = query.withLimit(2);
    }

    Publisher<List<T>> asList = Publishers.toList(session.execute(StandardOperations.Select.of(query)).publisher());
    Function<List<T>, List<T>> mapFn = list -> {
      validatorFn.accept(list);
      return list;
    };
    return Publishers.flatMapIterable(Publishers.map(asList, mapFn), x -> x);
  }

  @Override
  public Publisher<Boolean> exists() {
    Query query = this.query.withLimit(1); // check if at least one exists
    Publisher<List<T>> asList = Publishers.toList(session.execute(StandardOperations.Select.of(query)).publisher());
    return Publishers.map(asList, list -> !list.isEmpty());
  }

  @Override
  public Publisher<Long> count() {
    Query newQuery = query.withCount(true);
    return session.execute(StandardOperations.Select.of(newQuery)).publisher();
  }

  @Override
  public <X> ReactiveFetcher<X> map(Function<? super T, ? extends X> mapFn) {
    return new MappedFetcher<T, X>(this, mapFn);
  }

  @Override
  public ReactiveFetcher<T> changeQuery(UnaryOperator<Query> mapFn) {
    return new ReactiveFetcherDelegate<>(mapFn.apply(query), session);
  }

  private static class MappedFetcher<T, R> implements ReactiveFetcher<R> {

    private final ReactiveFetcher<T> delegate;
    private final Function<? super T, ? extends R> mapFn;

    private MappedFetcher(ReactiveFetcher<T> delegate, Function<? super T, ? extends R> mapFn) {
      this.delegate = Objects.requireNonNull(delegate, "delegate");
      this.mapFn = Objects.requireNonNull(mapFn, "mapFn");
    }

    private Publisher<R> map(Publisher<T> publisher) {
      return Publishers.map(publisher, mapFn);
    }

    @Override
    public Publisher<R> fetch() {
      return map(delegate.fetch());
    }

    @Override
    public Publisher<R> one() {
      return map(delegate.one());
    }

    @Override
    public Publisher<R> oneOrNone() {
      return map(delegate.oneOrNone());
    }

    @Override
    public Publisher<Boolean> exists() {
      return delegate.exists();
    }

    @Override
    public Publisher<Long> count() {
      return delegate.count();
    }

    @Override
    public <X> ReactiveFetcher<X> map(Function<? super R, ? extends X> mapFn) {
      return new MappedFetcher<>(this, mapFn);
    }

    @Override
    public ReactiveFetcher<R> changeQuery(UnaryOperator<Query> mapFn) {
      return new MappedFetcher<>(delegate.changeQuery(mapFn), this.mapFn);
    }
  }

  /**
   * Used to convert backend {@link ProjectedTuple} into repository specific {@link Tuple} interface
   */
  private static class TupleAdapter implements Tuple {

    private final ProjectedTuple delegate;

    private TupleAdapter(ProjectedTuple delegate) {
      this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Projection<T> projection) {
      return (T) delegate.get(Matchers.toExpression(projection));
    }

    @Override
    public List<?> values() {
      return delegate.values();
    }
  }

}
