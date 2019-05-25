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

import java.util.function.UnaryOperator;

/**
 * Creates document criteria from existing expression.
 */
public interface CriteriaCreator<R> {

  R create(CriteriaContext context);

  interface Factory {
    CriteriaContext context();
  }

  interface SingleFactory<T1> extends Factory {
    CriteriaCreator<T1> creator1();

    default T1 create1(UnaryOperator<Expression> operator) {
      return creator1().create(context().withOperator(operator));
    }

    default T1 create1() {
      return creator1().create(context());
    }
  }

  interface BiFactory<T1, T2> extends SingleFactory<T1> {

    CriteriaCreator<T2> creator2();

    default T2 create2(UnaryOperator<Expression> operator) {
      return creator2().create(context().withOperator(operator));
    }

    default T2 create2() {
      return creator2().create(context());
    }

  }

  interface TriFactory<T1, T2, T3>  extends BiFactory<T1, T2> {
    CriteriaCreator<T3> creator3();

    default T3 create3(UnaryOperator<Expression> operator) {
      return creator3().create(context().withOperator(operator));
    }

    default T3 create3() {
      return creator3().create(context());
    }

  }

  interface QuadFactory<T1, T2, T3, T4>  extends TriFactory<T1, T2, T3> {
    CriteriaCreator<T4> creator4();

    default T4 create4(UnaryOperator<Expression> operator) {
      return creator4().create(context().withOperator(operator));
    }

    default T4 create4() {
      return creator4().create(context());
    }

  }


  /**
   * Used to create a no-op creator
   */
  static <R> CriteriaCreator<R> unsupported() {
    return context -> {
      throw new UnsupportedOperationException("Not supposed to be called");
    };
  }

}
