package org.immutables.criteria.constraints;

import javax.annotation.Nullable;

/**
 * <p>Generic definition of expression.
 *
 * <p>Consider using sub-interfaces like {@link Literal} or {@link Call}
 * since they're more useful.
 */
public interface Expression<T> {

  @Nullable
  <R> R accept(ExpressionVisitor<R> visitor);

}