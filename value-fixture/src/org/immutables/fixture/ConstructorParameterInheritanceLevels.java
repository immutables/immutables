package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Enclosing
public enum ConstructorParameterInheritanceLevels {
  ;
  interface A {
    @Value.Parameter
    int a();
  }

  @Value.Immutable
  interface B extends A {
    @Value.Parameter
    int b();

    @Override
    @Value.Parameter
    int a();
  }

  interface C<T, V> {
    @Value.Parameter
    T a();

    @Value.Parameter
    V b();
  }

  @Value.Immutable
  interface D extends C<A, B> {}
}
