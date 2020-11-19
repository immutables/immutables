package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
public abstract class UnrelatedGenericMethods {
  public abstract String a();
  // should not be compilation error, not an accessor, can have type parameters
  public <T> T create() {
    return null;
  }

  @Value.Immutable
  interface Abb {
    @Value.Default
    default int x() {
      return 1;
    }
    // should not be compilation error, not an accessor, can have type parameters
    default <T> T create() {
      return null;
    }
  }
}
