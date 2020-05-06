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


import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operators;

/**
 * Very simple matcher for booleans has just {@code true} / {@code false} checks.
 *
 * @param <R> root criteria type
 */
public interface BooleanMatcher<R> extends Matcher {

  /**
   * Predicate {@code this == true}
   */
  default R isTrue() {
    return is(true);
  }

  /**
   * Predicate {@code this == false}
   */
  default R isFalse() {
    return is(false);
  }

  /**
   * Equivalent to {@code this == value}
   * @param value boolean value
   */
  default R is(boolean value) {
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.binaryCall(Operators.EQUAL, e, Expressions.constant(value)));
  }

  /**
   * For some interpreters (backends) {@code value != false} is not equivalent to {@code value == true}
   * (eg. Three-Valued-Logic). Creating separate method based on {@link Operators#NOT_EQUAL} operator.
   *
   * @see <a href="https://modern-sql.com/concept/three-valued-logic">Three Valued Logic</a>
   */
  default R isNot(boolean value) {
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.binaryCall(Operators.NOT_EQUAL, e, Expressions.constant(value)));
  }

  /**
   * Self-type for this matcher
   */
  interface Self extends Template<Self> {}

  interface Template<R> extends BooleanMatcher<R>, WithMatcher<R, Self>, NotMatcher<R, Self>, Projection<Boolean>, Aggregation.Count {}

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
