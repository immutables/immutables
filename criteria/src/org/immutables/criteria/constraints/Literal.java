package org.immutables.criteria.constraints;

/**
 * Constant value
 */
public interface Literal<T> extends Expression<T> {

  T value();

}
