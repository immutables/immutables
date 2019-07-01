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

public interface CollectionMatcher<R, S, C, V> {

  // createRoot = R (returns to createRoot criteria). createRoot / main
  // next = S (chains to next criteria). next / chain
  // with = C (accepts nested criteria). nested / inner
  default S all() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ALL, e);
    return Matchers.extract(this).<R, S, C>factory().createNested(expr);
  }

  default R all(UnaryOperator<C> consumer) {
    final CriteriaCreator.Factory<R, S, C> factory3 = Matchers.extract(this).<R, S, C>factory();
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ALL,
            Matchers.toExpressionOperator(factory3::createInner, consumer).apply(e));
    return factory3.createRoot(expr);
  }

  default S none() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.NONE, e);
    return Matchers.extract(this).<R, S, C>factory().createNested(expr);
  }

  default R none(UnaryOperator<C> consumer) {
    final CriteriaCreator.Factory<R, S, C> factory3 = Matchers.extract(this).<R, S, C>factory();
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.NONE,
            Matchers.toExpressionOperator(factory3::createInner, consumer).apply(e));
    return factory3.createRoot(expr);
  }

  default S any() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ANY, e);
    return Matchers.extract(this).<R, S, C>factory().createNested(expr);
  }

  default R any(UnaryOperator<C> consumer) {
    final CriteriaCreator.Factory<R, S, C> factory3 = Matchers.extract(this).<R, S, C>factory();
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ANY,
            Matchers.toExpressionOperator(factory3::createInner, consumer).apply(e));
    return factory3.createRoot(expr);
  }

  default S at(int index) {
    throw new UnsupportedOperationException();
  }

  default R contains(V value) {
    return Matchers.extract(this).<R, S, C>factory().createRoot(e -> Expressions.call(Operators.CONTAINS, e));
  }

  default R isEmpty() {
    return Matchers.extract(this).<R, S, C>factory().createRoot(e -> Expressions.call(Operators.EMPTY, e));
  }

  default R isNotEmpty() {
    return Matchers.extract(this).<R, S, C>factory().createRoot(e -> Expressions.not(Expressions.call(Operators.EMPTY, e)));
  }

  default R hasSize(int size) {
    UnaryOperator<Expression> expr = e -> Expressions.call(Operators.SIZE, e, Expressions.constant(size));
    return Matchers.extract(this).<R, S, C>factory().createRoot(expr);

  }


  interface Self<V> extends CollectionMatcher<Self, Self, Self, V> {}

}
