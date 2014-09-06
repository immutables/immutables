package org.immutables.generate.silly.nested;

import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.annotation.GenerateNested;

@GenerateNested
@GenerateMarshaler
class GroupedClasses {
  @GenerateImmutable
  interface NestedOne {}
}
