package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
public abstract class WitherDerived {
  abstract int set();

  @Value.Derived
  int derived() {
    return set() + 1;
  }
}
