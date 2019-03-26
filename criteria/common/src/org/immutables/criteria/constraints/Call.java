package org.immutables.criteria.constraints;

import java.util.List;

/**
 * An expression formed by a call to an operator (eg. {@link Operators#EQUAL}) with zero or more arguments.
 */
public interface Call<T> extends Expression<T> {

  /**
   * Get arguments of this operation
   */
  List<Expression<?>> getArguments();

  /**
   * Get the operator symbol for this operation
   *
   * @return operator
   */
  Operator getOperator();
}
