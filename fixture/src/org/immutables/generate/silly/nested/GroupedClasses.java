package org.immutables.generate.silly.nested;

import org.immutables.annotation.GenerateMarshaler;
import org.immutables.value.Value;

@Value.Nested
@GenerateMarshaler
class GroupedClasses {
  @Value.Immutable
  interface NestedOne {}
}
