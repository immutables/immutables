package org.immutables.criteria.constraints;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Objects;


class NullLiteral implements Literal {

  private final Class<?> type;

  private NullLiteral(Class<?> type) {
    this.type = Objects.requireNonNull(type,  "type");
  }

  static NullLiteral ofType(Class<?> type) {
    return new NullLiteral(type);
  }

  @Override
  public Object value() {
    return null;
  }

  @Override
  public Type valueType() {
    return type;
  }

  @Nullable
  @Override
  public <R, C> R accept(ExpressionVisitor<R, C> visitor, @Nullable C context) {
    return visitor.visit(this, context);
  }
}
