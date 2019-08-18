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


import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.ComparableOperators;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Criteria for comparables (like {@code >, <=, >} and ranges).
 *
 * @param <R> root criteria type
 */
public interface ComparableMatcher<R, V extends Comparable<? super V>> extends ObjectMatcher<R, V>, OrderMatcher {

  /**
   * Checks that attribute is less than (but not equal to) {@code upper} (equivalent to {@code $this < upper}).
   * <p>Use {@link #atMost(Comparable)} for less <i>or equal</i> comparison</p>
   */
  default R lessThan(V upper) {
    Objects.requireNonNull(upper, "upper");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(ComparableOperators.LESS_THAN, e, Expressions.constant(upper)));
  }

  /**
   * Checks that attribute is greater than (but not equal to)  {@code lower} (equivalent to {@code $this > lower}).
   * <p>Use {@link #atLeast(Comparable)} for greater <i>or equal</i> comparison</p>
   */
  default R greaterThan(V lower) {
    Objects.requireNonNull(lower, "lower");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(ComparableOperators.GREATER_THAN, e, Expressions.constant(lower)));
  }

  /**
   * Checks that attribute is less than or equal to {@code upperInclusive} (equivalent  to {@code $this <= upperInclusive}).
   */
  default R atMost(V upperInclusive) {
    Objects.requireNonNull(upperInclusive, "upperInclusive");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(ComparableOperators.LESS_THAN_OR_EQUAL, e, Expressions.constant(upperInclusive)));
  }

  /**
   * Checks that attribute is greater or equal to {@code lowerInclusive} (equivalent to {@code $this >= lowerInclusive}).
   */
  default R atLeast(V lowerInclusive) {
    Objects.requireNonNull(lowerInclusive, "lowerInclusive");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(ComparableOperators.GREATER_THAN_OR_EQUAL, e, Expressions.constant(lowerInclusive)));
  }

  /**
   * Checks that attribute is in {@code [lower, upper]} range (lower included, upper included).
   * (equivalent to {@code lowerInclusive <= $this <= upperInclusive})
   * @param lowerInclusive lower value (inclusive)
   * @param upperInclusive upper value (inclusive)
   */
  default R between(V lowerInclusive, V upperInclusive) {
    Objects.requireNonNull(lowerInclusive, "lowerInclusive");
    Objects.requireNonNull(upperInclusive, "upperInclusive");
    final UnaryOperator<Expression> unary = expr -> {
      final Call lower = Expressions.call(ComparableOperators.GREATER_THAN_OR_EQUAL, expr, Expressions.constant(lowerInclusive));
      final Call upper = Expressions.call(ComparableOperators.LESS_THAN_OR_EQUAL, expr, Expressions.constant(upperInclusive));
      return Expressions.and(lower, upper);
    };

    return Matchers.extract(this).applyAndCreateRoot(unary);
  }

  /**
   * Self-type for this matcher
   */
  interface Self<V extends Comparable<? super V>> extends Template<Self<V>, V>, Disjunction<Template<Self<V>, V>> {}

  interface Template<R, V extends Comparable<? super V>> extends ComparableMatcher<R, V>, WithMatcher<R, Self<V>>, NotMatcher<R, Self<V>>, Projection<V> {}

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
