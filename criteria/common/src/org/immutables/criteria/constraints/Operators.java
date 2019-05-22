package org.immutables.criteria.constraints;

public enum Operators implements Operator {

  EQUAL,
  NOT_EQUAL,

  // collection
  IN,
  NOT_IN,
  ALL,   // all elements match
  NONE, // no elements match
  ANY, // some elements match (at least one)
  EMPTY, // means collection is empty
  SIZE, // size of the collection

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
