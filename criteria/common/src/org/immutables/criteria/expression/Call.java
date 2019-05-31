package org.immutables.criteria.expression;

import java.util.List;

/**
 * An expression formed by a call to an operator (eg. {@link Operators#EQUAL}) with zero or more arguments.
 */
public interface Call extends Expression {

  /**
   * Get arguments of this operation
   */
  List<Expression> arguments();

  /**
   * Get the operator symbol for this operation
   *
   * @return operator
   */
  Operator operator();
}
