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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.ExpressionBiVisitor;
import org.immutables.criteria.expression.Expressions;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  <a href="https://en.wikipedia.org/wiki/Disjunctive_normal_form">Disjunctive normal form</a> (DNF)
 *  expression which enforces combination of simple expressions with {@code AND}s and {@code OR}s.
 *
 *  <p>Example: {@code (A and B or C and D or E)}
 */
class DnfExpression implements Expression {

  private final List<Expression> conjunctions; // ANDs
  private final List<Expression> disjunctions; // ORs

  DnfExpression() {
    this(ImmutableList.of(), ImmutableList.of());
  }

  private DnfExpression(List<Expression> conjunctions, List<Expression> disjunctions) {
    this.conjunctions = ImmutableList.copyOf(conjunctions);
    this.disjunctions = ImmutableList.copyOf(disjunctions);
  }

  boolean isEmpty() {
    return disjunctions.isEmpty() && conjunctions.isEmpty();
  }

  Expression simplify() {
    Preconditions.checkState(!isEmpty(), "no expression defined (conjunctions or disjunctions)");

    final List<Expression> expressions = new ArrayList<>(disjunctions);
    if (!conjunctions.isEmpty()) {
      expressions.add(Expressions.and(conjunctions));
    }

    return Expressions.or(expressions);
  }

  DnfExpression and(DnfExpression expression) {
    if (expression.isEmpty()) {
      return this;
    }

    if (expression.disjunctions.isEmpty()) {
      List<Expression> newConjunctions = ImmutableList.<Expression>builder().addAll(conjunctions).addAll(expression.conjunctions).build();
      return new DnfExpression(newConjunctions, disjunctions);
    }

    if (expression.conjunctions.isEmpty()) {
      List<Expression> newDisjunctions = ImmutableList.<Expression>builder().addAll(disjunctions).addAll(expression.disjunctions).build();
      return new DnfExpression(conjunctions, newDisjunctions);
    }

    return and((Expression) expression);
  }

  DnfExpression and(Expression expression) {
    Objects.requireNonNull(expression, "expression");
    ImmutableList<Expression> newConjunctions = ImmutableList.<Expression>builder().addAll(conjunctions).add(expression).build();
    return new DnfExpression(newConjunctions, disjunctions);
  }

  DnfExpression or() {
    List<Expression> newConjunctions = this.conjunctions.isEmpty() ? ImmutableList.of() : ImmutableList.of(Expressions.and(conjunctions));
    List<Expression> newDisjunctions = ImmutableList.<Expression>builder().addAll(disjunctions).addAll(newConjunctions).build();
    return new DnfExpression(ImmutableList.of(), newDisjunctions);
  }

  DnfExpression or(Expression expression) {
    Objects.requireNonNull(expression, "expression");
    List<Expression> newConjunctions = this.conjunctions.isEmpty() ? ImmutableList.of() : ImmutableList.of(Expressions.and(conjunctions));
    List<Expression> newDisjunctions = ImmutableList.<Expression>builder().addAll(disjunctions).addAll(newConjunctions).build();
    return new DnfExpression(ImmutableList.of(expression), newDisjunctions);
  }

  @Nullable
  @Override
  public <R, C> R accept(ExpressionBiVisitor<R, C> visitor, @Nullable C context) {
    return simplify().accept(visitor, context);
  }

  @Override
  public Type returnType() {
    return simplify().returnType();
  }
}
