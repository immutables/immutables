package org.immutables.generate.silly.nested;

import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.annotation.GenerateNested;

@GenerateNested
@GenerateImmutable
public interface InnerNested {
  @GenerateImmutable
  @GenerateMarshaler
  abstract static class Inner {}

  @GenerateImmutable
  @GenerateMarshaler
  interface Nested {}
}
