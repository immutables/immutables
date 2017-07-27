package org.immutables.fixture.builder.functional;

@FunctionalInterface
public interface CopyFunction<T extends AttributeBuilderValueI, U extends AttributeBuilderValueI> {
  T copy(U value);
}
