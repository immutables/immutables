package org.immutables.criteria.expression;

import java.util.Objects;

/**
 * Standard implementation of {@link Operator} interface. For now, used only
 * as a container.
 */
class SimpleOperator implements Operator {

  private final String name;
  private final Class<?> type;

  SimpleOperator(String name, Class<?> type) {
    this.name = Objects.requireNonNull(name, "name");
    this.type = Objects.requireNonNull(type, "type");
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Class<?> returnType() {
    return type;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    SimpleOperator that = (SimpleOperator) other;
    return Objects.equals(name, that.name) &&
            Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type);
  }
}
