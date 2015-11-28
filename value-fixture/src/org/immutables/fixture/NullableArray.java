package org.immutables.fixture;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface NullableArray {
  @Nullable
  byte[] array();
}
