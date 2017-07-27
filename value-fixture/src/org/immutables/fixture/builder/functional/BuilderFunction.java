package org.immutables.fixture.builder.functional;

@FunctionalInterface
public interface BuilderFunction<T> {
  AttributeBuilderBuilderI<T> newBuilder();
}
