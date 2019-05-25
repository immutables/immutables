package org.immutables.criteria.constraints;

import org.immutables.criteria.DocumentCriteria;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operators;

import java.util.Objects;
import java.util.function.UnaryOperator;

public class CollectionCriteria<R, S, C> implements DocumentCriteria<R> {

  private final CriteriaContext<R> context;
  private final CriteriaCreator<S> inner;
  private final CriteriaCreator<C> outer;

  public CollectionCriteria(CriteriaContext<R> context, CriteriaCreator<S> inner, CriteriaCreator<C> outer) {
    this.context = Objects.requireNonNull(context, "context");
    this.inner = Objects.requireNonNull(inner, "inner");
    this.outer = Objects.requireNonNull(outer, "outer");
  }

  public S all() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ALL, e);
    return inner.create((CriteriaContext<S>) context.create(expr));
  }

  public R all(UnaryOperator<C> consumer) {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ALL, toExpressionOperator(consumer).apply(e));
    return context.create(expr);
  }

  public S none() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.NONE, e);
    return inner.create((CriteriaContext<S>) context.create(expr));
  }

  public R none(UnaryOperator<C> consumer) {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.NONE, toExpressionOperator(consumer).apply(e));
    return context.create(expr);
  }

  public S any() {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ANY, e);
    return inner.create((CriteriaContext<S>) context.create(expr));
  }

  public R any(UnaryOperator<C> consumer) {
    final UnaryOperator<Expression> expr = e -> Expressions.call(Operators.ANY, toExpressionOperator(consumer).apply(e));
    return context.create(expr);
  }

  public S at(int index) {
    throw new UnsupportedOperationException();
  }

  public R isEmpty() {
    return context.create(e -> Expressions.call(Operators.EMPTY, e));
  }

  public R isNotEmpty() {
    return context.create(e -> Expressions.not(Expressions.call(Operators.EMPTY, e)));
  }

  public R hasSize(int size) {
    return context.create(e -> Expressions.call(Operators.SIZE, e, Expressions.constant(size)));
  }

  private UnaryOperator<Expression> toExpressionOperator(UnaryOperator<C> operator) {
    return expression -> {
      final C initial = context.withCreator(outer).create();
      final C changed = operator.apply(initial);
      return Expressions.extract(changed);
    };
  }

  public static class Self extends CollectionCriteria<Self, Self, Self> {
    public Self(CriteriaContext<Self> context) {
      super(context, Self::new, Self::new);
    }
  }

}
