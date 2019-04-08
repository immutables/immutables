package org.immutables.criteria.constraints;

import javax.annotation.Nullable;

/**
 * Sentinel for a null / nil / noop expression.
 *
 * <p>Not supposed to be visited at runtime</p>
 */
final class NilExpression<T> implements Expression<T> {

  static final NilExpression INSTANCE = new NilExpression();

  private NilExpression() {}

  @Nullable
  @Override
  public <R, C> R accept(ExpressionVisitor<R, C> visitor, @Nullable C context) {
    throw new UnsupportedOperationException(String.format("Can't visit %s", getClass().getSimpleName()));
  }

}
