package org.immutables.fixture;

import java.io.Serializable;
import org.immutables.value.Value;

public interface GenericInheritence {

  public static interface Gen<A extends Object & Comparable<A>, B extends Serializable> {
    A a();

    B b();
  }

  @Value.Immutable
  public static interface Sub1 extends Gen<String, Integer> {}

  @Value.Immutable
  public static interface Sub2 extends Gen<Long, Double> {}
}
