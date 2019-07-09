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
 * Allows chaining {@code NOT} operator:
 * 
 * <pre>
 *   {@code
 *     crit.not(f -> f.names.hasLength(2))
 *   }
 * </pre>
 */
public interface NotMatcher<R, C> {

  default R not(UnaryOperator<C> operator) {
    final CriteriaContext context = Matchers.extract(this);
    final CriteriaCreator.Factory<R, C> factory3 = context.<R, C>factory();
    final UnaryOperator<Expression> expr = e -> Expressions.not(Matchers.toInnerExpression(context, operator).apply(e));
    return factory3.createRoot(expr);

  }

}
