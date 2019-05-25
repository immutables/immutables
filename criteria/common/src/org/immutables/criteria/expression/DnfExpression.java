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
public class DnfExpression implements Expressional<DnfExpression>, Expression {

  private final List<Expression> conjunctions;
  private final List<Expression> disjunctions;

  private DnfExpression(List<Expression> conjunctions, List<Expression> disjunctions) {
    this.conjunctions = ImmutableList.copyOf(conjunctions);
    this.disjunctions = ImmutableList.copyOf(disjunctions);
  }

  public static DnfExpression create() {
    return new DnfExpression(Collections.emptyList(), Collections.emptyList());
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

    return Expressions.or(expressions);
  }

  public DnfExpression and(Expression expression) {
    Objects.requireNonNull(expression, "expression");
    ImmutableList<Expression> newConjunctions = ImmutableList.<Expression>builder().addAll(conjunctions).add(expression).build();
    return new DnfExpression(newConjunctions, disjunctions);
  }

  public DnfExpression or(Expression expression) {
    Objects.requireNonNull(expression, "expression");
    List<Expression> newDisjunction = ImmutableList.<Expression>builder().addAll(disjunctions).add(Expressions.and(conjunctions)).build();
    return new DnfExpression(ImmutableList.of(expression), newDisjunction);
  }

}
