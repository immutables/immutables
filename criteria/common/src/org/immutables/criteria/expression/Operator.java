package org.immutables.criteria.expression;

/**
 * Marker interface for now.
 * Operators can be {@code >}, {@code min}, {@code abs} etc.
 */
public interface Operator {

  /**
   * Name of the operator can be {@code =} or {@code <} etc.
   */
  String name();

  /**
   * Expected return type
   */
  Class<?> returnType();
}
