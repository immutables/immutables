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
package org.immutables.criteria.matcher;


import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operators;

/**
 * Criteria for comparables (like {@code >, <=, >} and ranges).
 */
public class ComparableMatcher<R, V extends Comparable<V>>
        extends ObjectMatcher<R, V> {

  public ComparableMatcher(CriteriaContext<R> context) {
    super(context);
  }

  /**]
   * Checks that attribute is less than (but not equal to) {@code upper}.
   * <p>Use {@link #isAtMost(Comparable)} for less <i>or equal</i> comparison</p>
   */
  public R isLessThan(V upper) {
    return create(e -> Expressions.call(Operators.LESS_THAN, e, Expressions.constant(upper)));
  }

  /**
   * Checks that attribute is greater than (but not equal to)  {@code lower}.
   * <p>Use {@link #isAtLeast(Comparable)} for greater <i>or equal</i> comparison</p>
   */
  public R isGreaterThan(V lower) {
    return create(e -> Expressions.call(Operators.GREATER_THAN, e, Expressions.constant(lower)));
  }

  /**
   * Checks that attribute is less than or equal to {@code upperInclusive}.
   */
  public R isAtMost(V upperInclusive) {
    return create(e -> Expressions.call(Operators.LESS_THAN_OR_EQUAL, e, Expressions.constant(upperInclusive)));
  }

  /**
   * Checks that attribute is greater or equal to {@code lowerInclusive}.
   */
  public R isAtLeast(V lowerInclusive) {
    return create(e -> Expressions.call(Operators.GREATER_THAN_OR_EQUAL, e, Expressions.constant(lowerInclusive)));
  }

  public static class Self<V extends Comparable<V>> extends ComparableMatcher<Self<V>, V> {
    public Self(CriteriaContext<ComparableMatcher.Self<V>> context) {
      super(context);
    }
  }

}
