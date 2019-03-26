package org.immutables.criteria.constraints;

/**
 * A constant. {@code true}, {@code 1}, {@code "foo"} etc.
 */
public interface Literal<T> extends Expression<T> {

  T value();

}
