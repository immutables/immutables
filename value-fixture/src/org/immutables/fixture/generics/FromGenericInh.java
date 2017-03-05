package org.immutables.fixture.generics;

import org.immutables.value.Value;

public interface FromGenericInh {
  public interface Bar extends Foo<Bar> {
    String getBarId();
//
//    @Override
//    Bar getFoo();
  }

  @Value.Immutable
  public interface Baz extends Bar {
    String getBazId();
  }

  public interface Foo<T> {
    T getFoo();
  }
}
