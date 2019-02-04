package org.immutables.fixture.generics;

import org.immutables.value.Value.Immutable;

public interface GenericCore {
  interface Core<T extends CharSequence> {
    T getObject();
  }

  @Immutable
  interface GenericChild<T extends CharSequence> extends Core<T> {}

  @Immutable
  interface GenericAdult<T extends CharSequence> extends Core<T> {
    int getId();
  }
}
