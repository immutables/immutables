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

package org.immutables.criteria.geode;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.Struct;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Query;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

class SyncSelect implements Callable<Iterable<Object>> {

  /**
   * Convert Geode specific {@link QueryService#UNDEFINED} value to null
   */
  private static final Function<Object, Object> UNDEFINED_TO_NULL = value -> QueryService.UNDEFINED.equals(value) ? null : value;

  private final GeodeBackend.Session session;
  private final StandardOperations.Select operation;

  SyncSelect(GeodeBackend.Session session, StandardOperations.Select operation) {
    this.session = session;
    this.operation = operation;
  }

  private static ProjectedTuple toTuple(Query query, Object value) {
    if (!(value instanceof Struct)) {
      // most likely single projection
      Preconditions.checkArgument(query.projections().size() == 1, "Expected single projection got %s", query.projections().size());
      Expression projection = query.projections().get(0);
      return ProjectedTuple.ofSingle(projection, UNDEFINED_TO_NULL.apply(value));
    }

    Struct struct = (Struct) value;
    List<Object> values = Arrays.stream(struct.getFieldValues()).map(UNDEFINED_TO_NULL).collect(Collectors.toList());
    return ProjectedTuple.of(query.projections(), values);
  }


  @Override
  public Iterable<Object> call() throws Exception {

    OqlWithVariables oql = session.toOql(operation.query(), true);
    if (GeodeBackend.logger.isLoggable(Level.FINE)) {
      GeodeBackend.logger.log(Level.FINE, "Querying Geode with {0}", oql);
    }

    // for projections use tuple function
    Function<Object, Object> tupleFn = operation.query().hasProjections() ? obj -> Geodes.castNumbers(toTuple(operation.query(), obj)) : x -> x;
    @SuppressWarnings("unchecked")
    Iterable<Object> result = (Iterable<Object>) session.queryService.newQuery(oql.oql()).execute(oql.variables().toArray(new Object[0]));
    // lazy transform
    return Iterables.transform(result, tupleFn::apply);
  }
}
