package org.immutables.criteria.constraints;

public enum Operators implements Operator {

  EQUAL,
  NOT_EQUAL,

  IN,
  NOT_IN,

  // boolean ops
  AND,
  OR,
  NOT,

  // vs is null / is not null ?
  IS_PRESENT,
  IS_ABSENT,

  // comparisons
  BETWEEN,
  GREATER_THAN,
  GREATER_THAN_OR_EQUAL,
  LESS_THAN,
  LESS_THAN_OR_EQUAL;

}
