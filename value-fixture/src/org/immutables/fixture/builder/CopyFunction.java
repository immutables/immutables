package org.immutables.fixture.builder;

@FunctionalInterface
public interface CopyFunction<T extends AttributeBuilderValueI, U extends AttributeBuilderValueI> {
  T copy(U value);
}
