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

import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.IterableOperators;

import java.util.function.UnaryOperator;

/**
 * Matcher on {@link Iterable} types. Has methods like {@code isEmpty()} / {@code notEmpty()}
 * and others.
 */
public interface IterableMatcher<R, S, V> extends Matcher {

  default R contains(V value) {
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(IterableOperators.CONTAINS, e, Expressions.constant(value)));
  }

  /**
   * Match iterable attribute to be empty (equivalent to {@code iterable.size() == 0})
   */
  default R isEmpty() {
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(IterableOperators.IS_EMPTY, e));
  }

  /**
   * Match iterable attribute to be NOT empty (equivalent to {@code iterable.size() > 0})
   */
  default R notEmpty() {
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(IterableOperators.NOT_EMPTY, e));
  }

  default R hasSize(int size) {
    UnaryOperator<Expression> expr = e -> Expressions.call(IterableOperators.HAS_SIZE, e, Expressions.constant(size));
    return Matchers.extract(this).applyAndCreateRoot(expr);

  }

  interface Self<R, V, P> extends Template<Self<R, V, P>, Self<R, V, P>, V, P>, Disjunction<Self<R, V, P>> {}

  interface Template<R, S, V, P> extends IterableMatcher<R, S, V>, Projection<P>, NotMatcher<R, Self<R, V, P>> {}

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
