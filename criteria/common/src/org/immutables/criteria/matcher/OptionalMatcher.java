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

import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operators;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Matcher for optional attributes
 */
public class OptionalMatcher<R, S, C>  {

  final CriteriaContext<R> context;

  private final CriteriaCreator<S> inner;

  private final CriteriaCreator<C> outer;

  public OptionalMatcher(CriteriaContext<R> context, CriteriaCreator<S> inner, CriteriaCreator<C> outer) {
   this.context = Objects.requireNonNull(context, "context");
   this.inner = Objects.requireNonNull(inner, "inner");
   this.outer = Objects.requireNonNull(outer, "outer");
  }

  public R isPresent() {
    return context.create(e -> Expressions.call(Operators.IS_PRESENT, e));
  }

  public R isAbsent() {
    return context.create(e -> Expressions.call(Operators.IS_ABSENT, e));
  }

  public S value() {
    return inner.create((CriteriaContext<S>) context);
  }

  public R value(UnaryOperator<C> consumer) {
    // convert UnaryOperator<C> into UnaryOperator<Expression>
    final UnaryOperator<Expression> fn = expression -> {
      final C initial = context.withCreator(outer).create();
      final C changed = consumer.apply(initial);
      return Matchers.extract(changed);
    };

    return context.create(fn);
  }

}
