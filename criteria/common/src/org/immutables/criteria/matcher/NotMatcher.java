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

import java.util.function.UnaryOperator;

/**
 * Equivalent to boolean {@code NOT} operator. Negates inner lambda expression.
 * 
 * <pre>
 *   {@code
 *     criteria.not(f -> f.name.startsWith("Jo"))
 *   }
 * </pre>
 * @param <R> root criteria type
 * @param <C> expression type inside lambda
 */
public interface NotMatcher<R, C> extends Matcher {

  /**
   * Negate (logically) lambda expression expressed by {@code operator}. Equivalent to boolean {@code NOT} operator.
   *
   * @param operator used as lambda expression
   * @return new root criteria with updated expression
   */
  default R not(UnaryOperator<C> operator) {
    final CriteriaContext context = Matchers.extract(this);
    final UnaryOperator<Expression> expr = e -> Expressions.not(Matchers.toInnerExpression(context, operator).apply(e));
    return context.applyAndCreateRoot(expr);
  }

}
