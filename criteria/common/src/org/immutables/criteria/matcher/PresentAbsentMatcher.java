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
import org.immutables.criteria.expression.OptionalOperators;

import java.util.function.UnaryOperator;

/**
 * Base interface for optional and optional-derived matchers having just two methods:
 * {@code isPresent()} and {@code isAbsent()}
 */
public interface PresentAbsentMatcher<R> extends Matcher {

  default R isPresent() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(OptionalOperators.IS_PRESENT, e);
    return Matchers.extract(this).applyAndCreateRoot(expr);
  }

  default R isAbsent() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(OptionalOperators.IS_ABSENT, e);
    return Matchers.extract(this).applyAndCreateRoot(expr);
  }
}
