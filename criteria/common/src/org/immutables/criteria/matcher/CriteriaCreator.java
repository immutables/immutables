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

import java.util.function.UnaryOperator;

/**
 * Creates document criteria from existing expression.
 */
public interface CriteriaCreator<R> {

  R create(CriteriaContext context);

  // root = R (returns to createRoot criteria). root
  // nested = S (chains to next criteria). nested
  // inner = C (accepts nested criteria). inner
  interface Factory<T1, T2>  {

    CriteriaContext context();

    CriteriaCreator<T1> root();

    CriteriaCreator<T2> nested();

    default T1 createRoot(UnaryOperator<Expression> operator) {
      return root().create(context().apply(operator));
    }

    default T1 createRoot() {
      return root().create(context());
    }

    default T2 createNested(UnaryOperator<Expression> operator) {
      return nested().create(context().apply(operator));
    }

    default T2 createNested() {
      return nested().create(context());
    }


  }

}
