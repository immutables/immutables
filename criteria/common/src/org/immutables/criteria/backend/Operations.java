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

package org.immutables.criteria.backend;

import com.google.common.collect.ImmutableList;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.expression.Query;
import org.immutables.value.Value;

import java.util.List;

/**
 * List of default operations which can be executed on the backend
 */
public final class Operations {

  private Operations() {}

  /**
   * Query sent to a backend similar to SQL {@code SELECT} clause.
   */
  @Value.Immutable
  public interface Select<T> extends Backend.Operation {

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
  public interface Insert<V> extends Backend.Operation {

    /**
     * List of values to be inserted
     */
    List<V> values();

    static <V> Insert<V> ofValues(Iterable<V> values) {
      return ImmutableInsert.<V>builder().values(ImmutableList.copyOf(values)).build();
    }

  }

  /**
   * Delete documents using some criteria
   */
  @Value.Immutable
  public interface Delete extends Backend.Operation {
    @Value.Parameter
    Query query();

    static Delete of(Criterion<?> criteria) {
      return ImmutableDelete.of(Criterias.toQuery(criteria));
    }
  }

  @Value.Immutable
  public interface Watch<T> extends Backend.Operation {
    @Value.Parameter
    Query query();

  }


}
