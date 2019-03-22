package org.immutables.criteria.constraints;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * A set of predefined factories for expressions like {@link Literal} or {@link Call}
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
      public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
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
      public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
      }
    };
  }


  public static <T> Call<T> call(final Operator operator, Expression<?> expression, Expression<?> ... operands) {
    return call(operator, expression, Arrays.asList(operands));
  }


  public static <T> Call<T> call(final Operator operator, Expression<?> expression, final Iterable<Expression<?>> operands) {
    final List<Expression<?>> ops = ImmutableList.<Expression<?>>builder().add(expression).addAll(operands).build();
    return new Call<T>() {
      @Override
      public List<Expression<?>> getOperands() {
        return ops;
      }

      @Override
      public Operator getOperator() {
        return operator;
      }

      @Nullable
      @Override
      public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visit(this);
      }
    };
  }

  /**
   * Used as {@code null} object
   */
  public static <T> Expression<T> empty() {
    return path("");
  }

}
