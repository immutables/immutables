package org.immutables.generator;

/**
 * Type should implement {@code Implicit} if it's implicitly mixable to (wrapper for) element of
 * type E
 * @param <E> wrapped element type
 */
public interface Implicit<E> {}
