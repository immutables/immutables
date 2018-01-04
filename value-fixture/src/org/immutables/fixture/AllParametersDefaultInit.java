package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(allParameters = true)
interface AllParametersDefaultInit {
  @Value.Default
  default String getFoo() {
    return "";
  }

  @Value.Default
  default boolean isBar() {
    return true;
  }
}
