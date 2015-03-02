package org.immutables.fixture;

import org.immutables.value.Value;

public interface GenericInheritence {

  public static interface Gen<A, B> {
    A a();

    B b();
  }

  @Value.Immutable
  public static interface Sub1 extends Gen<String, Integer> {}

  @Value.Immutable
  public static interface Sub2 extends Gen<Long, Object> {}
}
