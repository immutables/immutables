package org.immutables.fixture;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Modifiable
@Value.Immutable
@Value.Style(jdkOnly = true)
abstract class NullableRef {
  @Nullable
  abstract Object[] refs();
}

@Value.Immutable
public interface NullableArray {
  @Nullable
  byte[] array();
}
