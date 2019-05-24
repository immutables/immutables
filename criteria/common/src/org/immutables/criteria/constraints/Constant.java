package org.immutables.criteria.constraints;

import javax.annotation.Nullable;

/**
 * A constant. {@code true}, {@code 1}, {@code "foo"}, {@code null} etc.
 */
public final class Constant implements Expression {

  private final Object value;

  private Constant(Object value) {
    this.value = value;
  }

  public Object value() {
    return value;
  }

  public static Constant of(Object value) {
    return new Constant(value);
  }

  @Nullable
  @Override
  public <R, C> R accept(ExpressionBiVisitor<R, C> visitor, @Nullable C context) {
    return visitor.visit(this, context);
  }
}
