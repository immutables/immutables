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
 * Matcher on {@link Iterable} types. Has methods like {@code isEmpty()} / {@code isNotEmpty()}
 * and others.
 */
public interface IterableMatcher<R, S, C, V>  {

  default S all() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ALL, e);
    return Matchers.extract(this).<R, S>factory().createNested(expr);
  }

  default S none() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.NONE, e);
    return Matchers.extract(this).<R, S>factory().createNested(expr);
  }

  default S any() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ANY, e);
    return Matchers.extract(this).<R, S>factory().createNested(expr);
  }

  default S at(int index) {
    throw new UnsupportedOperationException();
  }

  default R contains(V value) {
    return Matchers.extract(this).<R, S>factory().createRoot(e -> Expressions.call(Operators.CONTAINS, e));
  }

  default R isEmpty() {
    return Matchers.extract(this).<R, S>factory().createRoot(e -> Expressions.call(Operators.EMPTY, e));
  }

  default R isNotEmpty() {
    return Matchers.extract(this).<R, S>factory().createRoot(e -> Expressions.not(Expressions.call(Operators.EMPTY, e)));
  }

  default R hasSize(int size) {
    UnaryOperator<Expression> expr = e -> Expressions.call(Operators.SIZE, e, Expressions.constant(size));
    return Matchers.extract(this).<R, S>factory().createRoot(expr);

  }

  interface Self<V> extends IterableMatcher<Self, Self, Self, V> {}

}
