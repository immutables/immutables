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

package org.immutables.criteria.expression;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *  <a href="https://en.wikipedia.org/wiki/Disjunctive_normal_form">Disjunctive normal form</a> (DNF)
 *  expression which enforces combination of simple expressions with {@code AND}s and {@code OR}s.
 *
 *  <p>Example: {@code (A and B or C and D or E)}
 */
public class DnfExpression implements Expressional, Expression {

  private final Query root;
  private final List<Expression> conjunctions;
  private final List<Expression> disjunctions;

  private DnfExpression(Query root, List<Expression> conjunctions, List<Expression> disjunctions) {
    this.root = root;
    this.conjunctions = ImmutableList.copyOf(conjunctions);
    this.disjunctions = ImmutableList.copyOf(disjunctions);
  }

  public static DnfExpression create(Query root) {
    return new DnfExpression(root, Collections.emptyList(), Collections.emptyList());
  }

  @Nullable
  @Override
  public <R, C> R accept(ExpressionBiVisitor<R, C> visitor, @Nullable C context) {
    return expression().accept(visitor, context);
  }

  @Override
  public Expression expression() {
    return simplify();
  }

  private Expression simplify() {
    final List<Expression> expressions = new ArrayList<>(disjunctions);
    if (!conjunctions.isEmpty()) {
      expressions.add(Expressions.and(conjunctions));
    }

    if (disjunctions.isEmpty() && conjunctions.isEmpty()) {
      return root;
    }

    return root.withFilter(Expressions.or(expressions));
  }

  public DnfExpression and(Expression expression) {
    Objects.requireNonNull(expression, "expression");
    ImmutableList<Expression> newConjunctions = ImmutableList.<Expression>builder().addAll(conjunctions).add(expression).build();
    return new DnfExpression(root, newConjunctions, disjunctions);
  }

  public DnfExpression or(Expression expression) {
    Objects.requireNonNull(expression, "expression");
    List<Expression> newDisjunction = ImmutableList.<Expression>builder().addAll(disjunctions).add(Expressions.and(conjunctions)).build();
    return new DnfExpression(root, ImmutableList.of(expression), newDisjunction);
  }

}
