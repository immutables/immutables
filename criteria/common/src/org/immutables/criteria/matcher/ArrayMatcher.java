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
import org.immutables.criteria.expression.Operators;

import java.util.function.UnaryOperator;

/**
 * Matcher on array types can delegate to {@link IterableMatcher}
 */
public interface ArrayMatcher<R, S, V> extends Matcher {

  /**
   * Convert to Iterable matcher which has richer API
   */
  default IterableMatcher<R, S, V> asList() {
    return IterableMatcher.<IterableMatcher<R, S, V>>creator().create(Matchers.extract(this));
  }

  default R hasLength(int length) {
    UnaryOperator<Expression> expr = e -> Expressions.call(Operators.SIZE, e, Expressions.constant(length));
    return Matchers.extract(this).<R, S>factory().createRoot(expr);
  }

  default R isEmpty() {
    return Matchers.extract(this).<R, S>factory().createRoot(e -> Expressions.call(Operators.EMPTY, e));
  }

  default R notEmpty() {
    return Matchers.extract(this).<R, S>factory().createRoot(e -> Expressions.not(Expressions.call(Operators.EMPTY, e)));
  }

  interface Self<R, V> extends ArrayMatcher<Self<R, V>, Self<R, V>, V>, Disjunction<Self<R, V>> {}

  @SuppressWarnings("unchecked")
  static <R> CriteriaCreator<R> creator() {
    class Local extends ContextHolder.AbstractHolder implements Self {
      private Local(CriteriaContext context) {
        super(context);
      }
    }

    return ctx -> (R) new Local(ctx);
  }

}
