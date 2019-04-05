package org.immutables.criteria.constraints;

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
class DnfExpression<T> implements Expression<T> {

  private final List<Expression<T>> conjunctions;
  private final List<Expression<T>> disjunctions;

  private DnfExpression(List<Expression<T>> conjunctions, List<Expression<T>> disjunctions) {
    this.conjunctions = ImmutableList.copyOf(conjunctions);
    this.disjunctions = ImmutableList.copyOf(disjunctions);
  }

  static <T> DnfExpression<T> create() {
    return new DnfExpression<>(Collections.emptyList(), Collections.emptyList());
  }

  @Nullable
  @Override
  public <R> R accept(ExpressionVisitor<R> visitor) {
    return simplify().accept(visitor);
  }

  private Expression<T> simplify() {
    final List<Expression<T>> expressions = new ArrayList<>(disjunctions);
    if (!conjunctions.isEmpty()) {
      expressions.add(Expressions.and(conjunctions));
    }

    return Expressions.or(expressions);
  }


  DnfExpression<T> and(Expression<T> expression) {
    Objects.requireNonNull(expression, "expression");
    ImmutableList<Expression<T>> newConjunctions = ImmutableList.<Expression<T>>builder().addAll(conjunctions).add(expression).build();
    return new DnfExpression<T>(newConjunctions, disjunctions);
  }

  DnfExpression<T> or(Expression<T> expression) {
    Objects.requireNonNull(expression, "expression");
    List<Expression<T>> newDisjunction = ImmutableList.<Expression<T>>builder().addAll(disjunctions).add(Expressions.and(conjunctions)).build();
    return new DnfExpression<>(ImmutableList.of(expression), newDisjunction);
  }

}
