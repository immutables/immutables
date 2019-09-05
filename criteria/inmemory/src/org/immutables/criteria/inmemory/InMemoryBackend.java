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

package org.immutables.criteria.inmemory;

import io.reactivex.Flowable;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.DefaultResult;
import org.immutables.criteria.backend.IdExtractor;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.expression.Collation;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Backend backed by a {@link Map} (usually {@link ConcurrentMap}).
 */
@SuppressWarnings("unchecked")
public class InMemoryBackend implements Backend {

  /** mapping between class and its store */
  private final ConcurrentMap<Class<?>, Map<Object, Object>> classToStore;

  public InMemoryBackend() {
    this.classToStore = new ConcurrentHashMap<>();
  }

  @Override
  public Session open(Class<?> entityType) {
    final Map<Object, Object> store = classToStore.computeIfAbsent(entityType, key -> new ConcurrentHashMap<>());
    return new Session(entityType, store);
  }

  private static class Session implements Backend.Session {

    private final Class<?> entityType;
    private final IdExtractor<Object, Object> idExtractor;
    private final Map<Object, Object> store;

    private Session(Class<?> entityType, Map<Object, Object> store) {
      this.entityType  = entityType;
      this.store = Objects.requireNonNull(store, "store");
      this.idExtractor = IdExtractor.reflection((Class<Object>) entityType);

    }

    @Override
    public Class<?> entityType() {
      return entityType;
    }

    @Override
    public Result execute(Operation operation) {
      return DefaultResult.of(executeInternal(operation));
    }

    private Publisher<?> executeInternal(Operation operation) {
      if (operation instanceof StandardOperations.Select) {
        return query((StandardOperations.Select<?>) operation);
      } else if (operation instanceof StandardOperations.Insert) {
        return insert((StandardOperations.Insert<?>) operation);
      }

      return Flowable.error(new UnsupportedOperationException(String.format("Operation %s not supported", operation)));
    }

    private <T> Publisher<T> query(StandardOperations.Select<T> select) {
      final Query query = select.query();
      if (query.hasAggregations()) {
        throw new UnsupportedOperationException("Aggregations are not yet supported by " + InMemoryBackend.class.getSimpleName());
      }
      Stream<T> stream = (Stream<T>) store.values().stream();

      // filter
      if (query.filter().isPresent()) {
        Predicate<T> predicate = InMemoryExpressionEvaluator.of(query.filter().get());
        stream = stream.filter(predicate);
      }

      // sort
      if (!query.collations().isEmpty()) {
        Comparator<T> comparator = null;
        for (Collation collation: query.collations()) {
          Function<T, Comparable<?>> fn = obj -> (Comparable<?>) new ReflectionFieldExtractor<>(obj).extract(collation.path());
          @SuppressWarnings("unchecked")
          Comparator<T> newComparator = Comparator.<T, Comparable>comparing(fn);
          if (!collation.direction().isAscending()) {
            newComparator = newComparator.reversed();
          }

          if (comparator != null) {
            comparator = comparator.thenComparing(newComparator);
          } else {
            comparator = newComparator;
          }
        }

        stream = stream.sorted(comparator);
      }

      if (query.hasProjections()) {
        final TupleExtractor extractor = new TupleExtractor(query);
        stream = (Stream<T>) stream.map(extractor::extract);
      }

      if (query.offset().isPresent()) {
        stream = stream.skip(query.offset().getAsLong());
      }

      if (query.limit().isPresent()) {
        stream = stream.limit(query.limit().getAsLong());
      }

      return Flowable.fromIterable(stream.collect(Collectors.toList()));
    }

    private <T> Publisher<WriteResult> insert(StandardOperations.Insert<T> op) {
      if (op.values().isEmpty()) {
        return Flowable.just(WriteResult.empty());
      }

      final Map<Object, T> toInsert = op.values().stream().collect(Collectors.toMap(idExtractor::extract, x -> x));
      @SuppressWarnings("unchecked")
      final Map<Object, T> store = (Map<Object, T>) this.store;
      return Flowable.fromCallable(() -> {
        store.putAll(toInsert);
        return WriteResult.unknown();
      });
    }
  }
}
