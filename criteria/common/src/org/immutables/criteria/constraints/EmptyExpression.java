package org.immutables.criteria.constraints;

import javax.annotation.Nullable;

/**
 * Sentinel for a null expression.
 */
final class EmptyExpression<T> implements Expression<T> {

  static final EmptyExpression INSTANCE = new EmptyExpression();

  private EmptyExpression() {}

  @Nullable
  @Override
  public <R> R accept(ExpressionVisitor<R> visitor) {
    throw new UnsupportedOperationException("Can't visit empty expression");
  }

}
