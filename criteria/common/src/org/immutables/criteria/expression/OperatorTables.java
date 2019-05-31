package org.immutables.criteria.expression;

import com.google.common.collect.ImmutableSet;

public final class OperatorTables {

  private OperatorTables() {}

  /**
   * List of operators which can be used on comparables (like numbers)
   */
  public static final ImmutableSet<Operator> COMPARISON = ImmutableSet.of(Operators.EQUAL,
          Operators.NOT_EQUAL, Operators.GREATER_THAN, Operators.GREATER_THAN_OR_EQUAL,
          Operators.LESS_THAN, Operators.LESS_THAN_OR_EQUAL, Operators.BETWEEN);


}
