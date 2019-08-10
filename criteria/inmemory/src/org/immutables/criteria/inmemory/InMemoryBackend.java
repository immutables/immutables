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
import org.immutables.criteria.backend.Backends;
import org.immutables.criteria.backend.EntityContext;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

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
public class InMemoryBackend implements Backend {

  /** mapping between class and its store */
  private final ConcurrentMap<Class<?>, Map<Object, Object>> classToStore;

  public InMemoryBackend() {
    this.classToStore = new ConcurrentHashMap<>();
  }

  @Override
  public Session open(Context context) {
    final Class<?> type = EntityContext.extractEntity(context);
    final Map<Object, Object> store = classToStore.computeIfAbsent(type, key -> new ConcurrentHashMap<>());
    return new Session(type, store);
  }

  private static class Session implements Backend.Session {

    private final Function<Object, Object> idExtractor;
    private final Map<Object, Object> store;

    private Session(Class<?> entityClass, Map<Object, Object> store) {
      this.store = Objects.requireNonNull(store, "store");
      this.idExtractor = Backends.idExtractor((Class<Object>) entityClass);

    }

    @Override
    public <T> Publisher<T> execute(Operation operation) {
      if (operation instanceof StandardOperations.Select) {
        return query((StandardOperations.Select<T>) operation);
      } else if (operation instanceof StandardOperations.Insert) {
        return (Publisher<T>) insert((StandardOperations.Insert<T>) operation);
      }

      return Flowable.error(new UnsupportedOperationException(String.format("Operation %s not supported", operation)));
    }

    private <T> Publisher<T> query(StandardOperations.Select<T> select) {
      final Query query = select.query();
      Stream<T> stream = (Stream<T>) store.values().stream();
      if (query.filter().isPresent()) {
        Predicate<T> predicate = InMemoryExpressionEvaluator.of(query.filter().get());

        stream = stream.filter(predicate);
      }

      if (!query.collations().isEmpty()) {
        throw new UnsupportedOperationException(String.format("%s does not support sorting: %s",
                InMemoryBackend.class.getSimpleName(),
                query.collations().stream().map(c -> c.path().toStringPath()).collect(Collectors.joining(", "))));
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
        return Flowable.just(WriteResult.UNKNOWN);
      }

      final Map<Object, T> toInsert = op.values().stream().collect(Collectors.toMap(idExtractor, x -> x));
      @SuppressWarnings("unchecked")
      final Map<Object, T> store = (Map<Object, T>) this.store;
      return Flowable.fromCallable(() -> {
        store.putAll(toInsert);
        return WriteResult.UNKNOWN;
      });
    }
  }
}
