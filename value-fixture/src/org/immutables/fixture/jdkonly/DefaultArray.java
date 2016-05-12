package org.immutables.fixture.jdkonly;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public abstract class DefaultArray {
  @Value.Default
  int[] prop() {
    return new int[0];
  }

  @Nullable
  @Value.Default
  int[] props() {
    return new int[0];
  }
}
