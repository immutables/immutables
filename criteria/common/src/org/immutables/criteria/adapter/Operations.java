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

package org.immutables.criteria.adapter;

import com.google.common.collect.ImmutableList;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.DocumentCriteria;
import org.immutables.criteria.WriteResult;
import org.immutables.criteria.expression.Query;
import org.immutables.value.Value;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * List of default operations which can be executed on the backend
 */
public final class Operations {

  private Operations() {}

  /**
   * Query sent to a backend similar to SQL {@code SELECT} clause.
   */
  @Value.Immutable
  public interface Select<T> extends Backend.Operation<T> {

    @Value.Parameter
    Query query();

    static <T> ImmutableSelect<T> of(Query query) {
      return ImmutableSelect.of(query);
    }
  }

  /**
   * Insert operation for a list of objects.
   */
  @Value.Immutable
  public interface Insert<V> extends Backend.Operation<WriteResult> {

    /**
     * List of values to be inserted
     */
    List<V> values();

    static <V> Insert<V> ofValues(Iterable<V> values) {
      return ImmutableInsert.<V>builder().values(ImmutableList.copyOf(values)).build();
    }

    /**
     * Create an insert where objects have ID.
     */
    static <K, V> KeyedInsert<K, V> ofEntries(Iterable<Map.Entry<K, V>> entries) {
      return ImmutableKeyedInsert.<K, V>builder().entries(entries).build();
    }

    /**
     * Extract IDs using provided functions and create Insert with {@link #ofEntries(Iterable)} ()}.
     */
    static <K, V> KeyedInsert<K, V> ofKeyed(Iterable<V> iterable, Function<? super V, K> keyExtractor) {
      final List<Map.Entry<K, V>> entries = StreamSupport.stream(iterable.spliterator(), false)
              .map(d -> new AbstractMap.SimpleImmutableEntry<>(keyExtractor.apply(d), d))
              .collect(Collectors.toList());

      return ofEntries(entries);
    }
  }

  /**
   * Insert operation for a set of objects which have keys (aka {@code ID}). This is usually used
   * for key-value stores (like Geode / {@link java.util.concurrent.ConcurrentMap}) where one needs
   * to provide key explicitly (like {@link Map#put(Object, Object)} in API.
   *
   * @param <K> key type of the key
   * @param <V> value type of the value
   */
  @Value.Immutable
  public interface KeyedInsert<K, V> extends Insert<V> {

    List<Map.Entry<K, V>> entries();

    /**
     * Convert events to a map (assumes no duplicate keys)
     */
    default Map<K, V> toMap() {
      return entries().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    default List<V> values() {
      return entries().stream().map(Map.Entry::getValue).collect(Collectors.toList());
    }
  }

  /**
   * Delete documents using some criteria
   */
  @Value.Immutable
  public interface Delete extends Backend.Operation<WriteResult> {
    @Value.Parameter
    Query query();

    static Delete of(DocumentCriteria<?> criteria) {
      return ImmutableDelete.of(Criterias.toQuery(criteria));
    }
  }

  @Value.Immutable
  public interface Watch<T> extends Backend.Operation<T> {
    @Value.Parameter
    Query query();

  }


}
