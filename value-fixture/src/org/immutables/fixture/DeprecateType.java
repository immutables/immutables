package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable(singleton = true)
@Deprecated
interface DeprecateType {

  default void use() {
    ImmutableDeprecateType.of();
  }
}
