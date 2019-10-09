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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.expression.Visitors;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Common operations which can be executed on a backend.
 * It is by no means an exhaustive list and other (custom) operations can exists independently.
 */
public final class StandardOperations {

  private StandardOperations() {}

  /**
   * Query sent to a backend similar to SQL {@code SELECT} clause.
   */
  @Value.Immutable
  public interface Select extends Backend.Operation {

    @Value.Parameter
    Query query();

    static ImmutableSelect of(Query query) {
      return ImmutableSelect.of(query);
    }
  }

  /**
   * Insert operation for a list of objects.
   */
  @Value.Immutable
  public interface Insert extends Backend.Operation {

    /**
     * List of values to be inserted
     */
    @Value.Parameter
    List<?> values();

    static Insert ofValues(Iterable<?> values) {
      return ImmutableInsert.of(ImmutableList.copyOf(values));
    }

  }

  /**
   * Conditional update operation similar to SQL clause {@code UPDATE ... SET ... WHERE ...}.
   */
  @Value.Immutable
  public interface Update extends Backend.Operation {

    /**
     * Filter for records to be updated
     */
    @Value.Parameter
    Query query();

    /**
     * Values to be set. Key is usually a {@linkplain Path} and value is object to be set.
     */
    @Value.Parameter
    Map<Expression, Object> values();

    /**
     * If current operation is replace, return replacement, otherwise return empty optional.
     * Replace means override whole record / document not a subset of attributes.
     */
    default Optional<Object> replace() {
      Optional<Path> found = values().keySet().stream().map(Visitors::toPath).filter(Path::isEntityPath).findAny();
      // check that there is just a replace value. no others (like attribute set)
      found.ifPresent(p -> {
        if (values().size() != 1) {
          throw new IllegalArgumentException(String.format("Expected exactly one value for replacement got %d: %s", values().size(), values().keySet()));
        }
      });
      return found.map(p -> values().get(p));
    }

    static Update of(Query query, Map<Expression, Object> values) {
      Preconditions.checkArgument(!values.isEmpty(), "no values");
      return ImmutableUpdate.of(query, values);
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
  public interface Watch extends Backend.Operation {
    @Value.Parameter
    Query query();
  }


}
