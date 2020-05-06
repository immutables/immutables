/*
 * Copyright 2020 Immutables Authors and Contributors
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

import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.Operators;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Combines two expressions, simplifying if possible
 */
interface Combiner {

  Expression combine(Expression left, Expression right);

  static Combiner and() {
    return nullSafe((left, right) -> combineAndSimplify(Operators.AND, left, right));
  }

  static Combiner or() {
    return nullSafe((left, right) -> combineAndSimplify(Operators.OR, left, right));
  }

  /**
   * Combines expression using <a href="https://en.wikipedia.org/wiki/Disjunctive_normal_form">DNF logic</a>
   */
  static Combiner dnfAnd() {
    Combiner combiner = new Combiner() {
      @Override
      public Expression combine(Expression left, Expression right) {
        if (!(left instanceof Call)) {
          // regular and
          return and().combine(left, right);
        }

        Call root = (Call) left;
        if (root.operator() == Operators.OR) {
          // change right-most child
          // a.is(1)
          // .or()
          // .b.is(2).c.is(3)
          List<Expression> args = root.arguments().stream()
                  .limit(root.arguments().size() - 1) // skip last
                  .collect(Collectors.toList());
          Expression last = root.arguments().get(root.arguments().size() - 1);
          Expression newLast = combine(last, right);
          args.add(newLast);
          return Expressions.or(args);
        }

        // simple 2 arg AND call
        return and().combine(left, right);
      }
    };

    return nullSafe(combiner);
  }

  static Combiner nullSafe(Combiner delegate) {
    Objects.requireNonNull(delegate, "delegate");

    return (left, right) -> {
      if (left == null) {
        return right;
      }

      if (right == null) {
        return left;
      }

      return delegate.combine(left, right);
    };
  }

  static Call combineAndSimplify(Operator operator, Expression leftExpr, Expression rightExpr) {
    Objects.requireNonNull(operator, "operator");
    if (!(leftExpr instanceof Call && rightExpr instanceof Call)) {
      // regular call which can't be simplified
      return Expressions.binaryCall(operator, leftExpr, rightExpr);
    }

    Call left = (Call) leftExpr;
    Call right = (Call) rightExpr;

    if (!(left.operator() == operator && right.operator() == operator)) {
      if (left.operator() == operator) {
        List<Expression> args = new ArrayList<>(left.arguments());
        args.add(right);
        return Expressions.call(operator, args);
      }

      return Expressions.binaryCall(operator, left, right);
    }

    // combine expressions with same operator (AND / OR) into single expression
    // with multiple arguments
    List<Expression> args = new ArrayList<>();
    args.addAll(left.arguments());
    args.addAll(right.arguments());
    return Expressions.call(operator, args);
  }
}
