package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable(builder = false)
class NoWarningInnerBuilderEnum {
  // inner builder is enum, but it is ignored
  // with no warning as the builder is disabled
  enum Builder {}
}
