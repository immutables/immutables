package org.immutables.fixture.nullable;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(defaultAsDefault = true)
public interface NullableDefault {
  @Nullable
  default String str() {
    return "def";
  }

  @Nullable
  default Integer intr() {
    return -1;
  }
}
