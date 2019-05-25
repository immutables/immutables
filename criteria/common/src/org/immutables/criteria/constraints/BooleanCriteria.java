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
package org.immutables.criteria.constraints;


import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operators;

/**
 * Very simple criteria for booleans just has {@code true} / {@code false} checks.
 */
public class BooleanCriteria<R> extends ObjectCriteria<R, Boolean> {

  public BooleanCriteria(CriteriaContext<R> context) {
    super(context);
  }

  public R isTrue() {
    return create(e -> Expressions.call(Operators.EQUAL, e, Expressions.constant(Boolean.TRUE)));
  }

  public R isFalse() {
    return create(e -> Expressions.call(Operators.EQUAL, e, Expressions.constant(Boolean.FALSE)));
  }

  public static class Self extends BooleanCriteria<Self> {
    public Self(CriteriaContext<BooleanCriteria.Self> context) {
      super(context);
    }
  }

}
