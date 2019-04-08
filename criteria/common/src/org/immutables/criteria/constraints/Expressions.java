package org.immutables.criteria.constraints;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * A set of predefined utilities and factories for expressions like {@link Literal} or {@link Call}
 */
public final class Expressions {

  private Expressions() {}

  public static <T> Path<T> path(final String path) {
    Preconditions.checkNotNull(path, "path");

    return new Path<T>() {
      @Override
      public String path() {
        return path;
      }

      @Nullable
      @Override
      public <R, C> R accept(ExpressionVisitor<R, C> visitor, @Nullable C context) {
        return visitor.visit(this, context);
      }
    };
  }

  public static <T> Literal<T> literal(final T value) {
    return new Literal<T>() {
      @Override
      public T value() {
        return value;
      }

      @Nullable
      @Override
      public <R, C> R accept(ExpressionVisitor<R, C> visitor, @Nullable C context) {
        return visitor.visit(this, context);
      }
    };
  }

  public static <T> Expression<T> and(Expression<T> first, Expression<T> second) {
    return and(Arrays.asList(first, second));
  }

  public static <T> Expression<T> and(Iterable<? extends Expression<T>> expressions) {
    return reduce(Operators.AND, expressions);
  }

  public static <T> Expression<T> or(Expression<T> first, Expression<T> second) {
    return or(Arrays.asList(first ,second));
  }

  public static <T> Expression<T> or(Iterable<? extends Expression<T>> expressions) {
    return reduce(Operators.OR, expressions);
  }

  private static <T> Expression<T> reduce(Operator operator, Iterable<? extends Expression<T>> expressions) {
    final Iterable<? extends Expression<T>> filtered = Iterables.filter(expressions, e -> !isNil(e) );
    final int size = Iterables.size(filtered);

    if (size == 0) {
      return nil();
    } else if (size == 1) {
      return filtered.iterator().next();
    }

    return call(operator, expressions);
  }

  /**
   * Combines expressions <a href="https://en.wikipedia.org/wiki/Disjunctive_normal_form">Disjunctive normal form</a>
   */
  public static <T> Expression<T> dnf(Operator operator, Expression<T> existing, Expression<T> newExpression) {
    if (!(operator == Operators.AND || operator == Operators.OR)) {
      throw new IllegalArgumentException(String.format("Expected %s for operator but got %s",
              Arrays.asList(Operators.AND, Operators.OR), operator));
    }

    if (isNil(existing)) {
      return DnfExpression.<T>create().and(newExpression);
    }

    if (!(existing instanceof DnfExpression)) {
      throw new IllegalStateException(String.format("Expected existing expression to be %s but was %s",
              DnfExpression.class.getName(), existing.getClass().getName()));
    }

    @SuppressWarnings("unchecked")
    final DnfExpression<T> conjunction = (DnfExpression<T>) existing;
    return operator == Operators.AND ? conjunction.and(newExpression) : conjunction.or(newExpression);
  }

  public static <T> Call<T> call(final Operator operator, Expression<?> ... operands) {
    return call(operator, Arrays.asList(operands));
  }

  public static <T> Call<T> call(final Operator operator, final Iterable<? extends Expression<?>> operands) {
    final List<Expression<?>> ops = ImmutableList.copyOf(operands);
    return new Call<T>() {
      @Override
      public List<Expression<?>> getArguments() {
        return ops;
      }

      @Override
      public Operator getOperator() {
        return operator;
      }

      @Nullable
      @Override
      public <R, C> R accept(ExpressionVisitor<R, C> visitor, @Nullable C context) {
        return visitor.visit(this, context);
      }
    };
  }

  /**
   * Used as sentinel for {@code noop} expression.
   */
  @SuppressWarnings("unchecked")
  public static <T> Expression<T> nil() {
    return (Expression<T>) NilExpression.INSTANCE;
  }

  public static boolean isNil(Expression<?> expression) {
    return expression == NilExpression.INSTANCE;
  }

}
