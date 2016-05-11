package org.immutables.fixture.jdkonly;

import org.immutables.value.Value;

@Value.Immutable
public abstract class DefaultArray {
  @Value.Default
  int[] prop() {
    return new int[0];
  }
}
