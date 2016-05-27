package org.immutables.fixture.generics;

import org.immutables.value.Value;

// Compilation test to nail out generic raw types
// instanceof in builder.from
public interface GenericSubType {

  interface A {
    int a();
  }

  @Value.Immutable
  interface B<T> extends A {
    T b();
  }
}
