package org.immutables.fixture.builder;

@FunctionalInterface
public interface BuilderFunction<T> {
  AttributeBuilderBuilderI<T> newBuilder();
}
