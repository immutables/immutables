package org.immutables.criteria.constraints;

import java.util.List;

/**
 * An expression formed by a call to an operator (eg. {@link Operators#EQUAL}) with zero or more arguments.
 */
public interface Call<T> extends Expression<T> {


  /**
   * Get the arguments of this operation
   *
   * @return arguments
   */
  List<Expression<?>> getOperands();

  /**
   * Get the operator symbol for this operation
   *
   * @return operator
   */
  Operator getOperator();
}
