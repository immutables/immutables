package org.immutables.criteria.constraints;

/**
 * Access to a field.
 * 
 * @param <T>
 */
public interface Path<T> extends Expression<T> {

  String path();

}
