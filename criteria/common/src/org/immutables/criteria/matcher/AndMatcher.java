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

import org.immutables.criteria.Criterion;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;

import java.util.function.UnaryOperator;

/**
 * Combines matchers using logical {@code AND}
 * @param <R> root criteria type
 */
public interface AndMatcher<R extends Criterion<?>> {

  /**
   * Combine {@code this} and {@code other} expression (criteria / matcher) using logical {@code AND}
   * operator. Equivalent to {@code this AND other}.
   *
   * @param other other matcher
   * @return new root criteria with updated expression
   */
  default R and(R other) {
    final CriteriaContext context = Matchers.extract(this);
    final UnaryOperator<Expression> expr = e -> Expressions.and(Matchers.concat(e, other));
    return context.<R, Object>factory().createRoot(expr);
  }

}
