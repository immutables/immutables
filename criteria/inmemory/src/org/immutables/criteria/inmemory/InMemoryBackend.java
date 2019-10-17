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
import org.immutables.criteria.backend.BackendException;
import org.immutables.criteria.backend.DefaultResult;
import org.immutables.criteria.backend.IdExtractor;
import org.immutables.criteria.backend.IdResolver;
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
  private final IdResolver idResolver;

  public InMemoryBackend() {
    this(InMemorySetup.of());
  }

  public InMemoryBackend(InMemorySetup setup) {
    Objects.requireNonNull(setup, "setup");
    this.idResolver = setup.idResolver();
    this.classToStore = new ConcurrentHashMap<>();
  }

  @Override
  public Session open(Class<?> entityType) {
    final Map<Object, Object> store = classToStore.computeIfAbsent(entityType, key -> new ConcurrentHashMap<>());
    IdExtractor extractor = IdExtractor.fromResolver(idResolver);
    return new Session(entityType, extractor, store);
  }

  private static class Session implements Backend.Session {

    private final Class<?> entityType;
    private final PathExtractor pathExtractor;
    private final IdExtractor idExtractor;
    private final Map<Object, Object> store;

    private Session(Class<?> entityType, IdExtractor extractor, Map<Object, Object> store) {
      this.entityType  = entityType;
      this.store = Objects.requireNonNull(store, "store");
      this.idExtractor = extractor;
      this.pathExtractor = new ReflectionExtractor();
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
        return query((StandardOperations.Select) operation);
      } else if (operation instanceof StandardOperations.Insert) {
        return insert((StandardOperations.Insert) operation);
      } else if (operation instanceof StandardOperations.Update) {
        return update((StandardOperations.Update) operation);
      }

      return Flowable.error(new UnsupportedOperationException(String.format("Operation %s not supported", operation)));
    }

    private Publisher<?> query(StandardOperations.Select select) {
      final Query query = select.query();
      if (query.hasAggregations()) {
        throw new UnsupportedOperationException("Aggregations are not yet supported by " + InMemoryBackend.class.getSimpleName());
      }
      Stream<Object> stream = store.values().stream();

      // filter
      if (query.filter().isPresent()) {
        Predicate<Object> predicate = InMemoryExpressionEvaluator.of(query.filter().get());
        stream = stream.filter(predicate);
      }

      // sort
      if (!query.collations().isEmpty()) {
        Comparator<Object> comparator = null;
        for (Collation collation: query.collations()) {
          Function<Object, Comparable<Object>> fn = obj -> (Comparable<Object>) pathExtractor.extract(collation.path(), obj);
          @SuppressWarnings("unchecked")
          Comparator<Object> newComparator = Comparator.<Object, Comparable>comparing(fn);
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
        final TupleExtractor extractor = new TupleExtractor(query, pathExtractor);
        stream = stream.map(extractor::extract);
      }

      if (query.offset().isPresent()) {
        stream = stream.skip(query.offset().getAsLong());
      }

      if (query.limit().isPresent()) {
        stream = stream.limit(query.limit().getAsLong());
      }

      if (query.count()) {
        // just return count
        return Flowable.just(stream.count());
      }

      return Flowable.fromIterable(stream.collect(Collectors.toList()));
    }

    private Publisher<WriteResult> update(StandardOperations.Update op) {
      if (op.values().isEmpty()) {
        return Flowable.just(WriteResult.empty());
      }

      Map<Object, Object> toUpdate = op.values().stream().collect(Collectors.toMap(idExtractor::extract, v -> v));

      if (op.upsert()) {
        return Flowable.fromCallable(() -> {
          store.putAll(toUpdate);
          return WriteResult.unknown();
        });
      }

      return Flowable.fromCallable(() -> {
        toUpdate.forEach(store::replace);
        return WriteResult.unknown();
      });
    }

    private Publisher<WriteResult> insert(StandardOperations.Insert op) {
      if (op.values().isEmpty()) {
        return Flowable.just(WriteResult.empty());
      }

      final Map<Object, Object> toInsert = op.values().stream().collect(Collectors.toMap(idExtractor::extract, x -> x));
      toInsert.forEach((k, v) -> {
        Object result = store.putIfAbsent(k, v);
        if (result != null) {
          throw new BackendException(String.format("Duplicate key %s for %s", k, entityType()));
        }
      });

      return Flowable.just(WriteResult.unknown());
    }
  }
}
