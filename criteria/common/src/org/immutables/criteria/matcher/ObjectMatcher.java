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

package org.immutables.criteria.matcher;

import com.google.common.collect.ImmutableList;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Predicates for most generic type {@code Object}. Typically used when there is no more specific
 * matcher like {@link ComparableMatcher}.
 *
 * @param <V> attribute type for which criteria is applied
 * @param <R> Criteria self-type, allowing {@code this}-returning methods to avoid needing subclassing
 */
public interface ObjectMatcher<R, V> extends Matcher {

  /**
   * Equivalent to {@code this == value} ({@code value} can't be null)
   * @throws NullPointerException if value argument is null
   */
  default R is(V value) {
    Objects.requireNonNull(value,"value");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.binaryCall(Operators.EQUAL, e, Expressions.constant(value)));
  }

  /**
   * Equivalent to {@code this != value} ({@code value} can't be null)
   * @throws NullPointerException if value argument is null
   */
  default R isNot(V value) {
    Objects.requireNonNull(value,"value");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.binaryCall(Operators.NOT_EQUAL, e, Expressions.constant(value)));
  }

  default R in(V v1, V v2, V ... rest) {
    final List<V> values = new ArrayList<>(2 + rest.length);
    values.add(v1);
    values.add(v2);
    values.addAll(Arrays.asList(rest));

    return in(values);
  }

  default R notIn(V v1, V v2, V ... rest) {
    final List<V> values = new ArrayList<>(2 + rest.length);
    values.add(v1);
    values.add(v2);
    values.addAll(Arrays.asList(rest));

    return notIn(values);
  }

  /**
   * Equivalent to {@code this in $values}
   */
  default R in(Iterable<? extends V> values) {
    Objects.requireNonNull(values, "values");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.binaryCall(Operators.IN, e, Expressions.constant(ImmutableList.copyOf(values))));
  }

  default R notIn(Iterable<? extends V> values) {
    Objects.requireNonNull(values, "values");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.binaryCall(Operators.NOT_IN, e, Expressions.constant(ImmutableList.copyOf(values))));
  }

  /**
   * Self-type for this matcher
   */
  interface Self<V> extends Template<Self<V>, V>, Disjunction<Template<Self<V>, V>> {}

  interface Template<R, V> extends ObjectMatcher<R, V>, WithMatcher<R, Self<V>>, NotMatcher<R, Self<V>>, Projection<V>, AggregationTemplate<V> {}

  interface AggregationTemplate<V> extends Aggregation.Count {}

  @SuppressWarnings("unchecked")
  static <R> CriteriaCreator<R> creator() {
    class Local extends AbstractContextHolder implements Self {
      private Local(CriteriaContext context) {
        super(context);
      }
    }

    return ctx -> (R) new Local(ctx);
  }


}
