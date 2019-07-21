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

import java.util.Objects;

/**
 * Very simple matcher for booleans just has {@code true} / {@code false} checks.
 */
public interface BooleanMatcher<R>  {

  default R isTrue() {
    return Matchers.extract(this).<R, Object>factory()
            .createRoot(e -> Expressions.call(Operators.EQUAL, e, Expressions.constant(Boolean.TRUE)));
  }

  default R isFalse() {
    return Matchers.extract(this).<R, Object>factory()
            .createRoot(e -> Expressions.call(Operators.EQUAL, e, Expressions.constant(Boolean.FALSE)));
  }

  interface Self extends Template<Self> {}

  interface With<R> extends WithMatcher<R, Self> {}

  interface Not<R> extends NotMatcher<R, Self> {}

  interface Template<R> extends BooleanMatcher<R>, With<R>, Not<R> {}

  @SuppressWarnings("unchecked")
  static <R> CriteriaCreator<R> creator() {
    class Local extends HasContext.Holder implements Self {
      private Local(CriteriaContext context) {
        super(context);
      }
    }

    return ctx -> (R) new Local(ctx);
  }

}
