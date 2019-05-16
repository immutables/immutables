package org.immutables.criteria.constraints;

import java.lang.reflect.Type;

/**
 * A constant. {@code true}, {@code 1}, {@code "foo"}, {@code null} etc.
 */
public interface Literal extends Expression {

  Object value();

  Type valueType();

}
