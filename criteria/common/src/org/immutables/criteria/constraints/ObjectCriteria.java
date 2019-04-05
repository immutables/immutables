/*
   Copyright 2013-2018 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package org.immutables.criteria.constraints;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.immutables.criteria.DocumentCriteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Comparing directly values of an attribute.
 *
 * @param <V> attribute type for which criteria is applied
 * @param <R> Criteria self-type, allowing {@code this}-returning methods to avoid needing subclassing
 */
public class ObjectCriteria<R extends DocumentCriteria<R>, V> {

  private final CriteriaContext<R> context;

  ObjectCriteria(CriteriaContext<R> context) {
    this.context = Preconditions.checkNotNull(context, "context");
  }

  /**
   * Use context to create new root DocumentCriteria
   */
  protected R create(UnaryOperator<Expression<?>> fn) {
    return (R) context.create(fn);
  }

  public R isEqualTo(V value) {
    return create(e -> Expressions.call(Operators.EQUAL, e, Expressions.literal(value)));
  }

  public R isNotEqualTo(V value) {
    return create(e -> Expressions.call(Operators.NOT_EQUAL, e, Expressions.literal(value)));
  }

  public R isIn(V v1, V v2, V ... rest) {
    final List<V> values = new ArrayList<>(2 + rest.length);
    values.add(v1);
    values.add(v2);
    values.addAll(Arrays.asList(rest));

    return isIn(values);
  }

  public R isNotIn(V v1, V v2, V ... rest) {
    final List<V> values = new ArrayList<>(2 + rest.length);
    values.add(v1);
    values.add(v2);
    values.addAll(Arrays.asList(rest));

    return isNotIn(values);
  }

  public R isIn(Iterable<? super V> values) {
    Preconditions.checkNotNull(values, "values");
    return create(e -> Expressions.call(Operators.IN, e, Expressions.literal(ImmutableList.copyOf(values))));
  }

  public R isNotIn(Iterable<? super V> values) {
    Preconditions.checkNotNull(values, "values");
    return create(e -> Expressions.call(Operators.NOT_IN, e, Expressions.literal(ImmutableList.copyOf(values))));
  }

}
