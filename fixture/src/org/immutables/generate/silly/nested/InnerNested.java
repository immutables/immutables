package org.immutables.generate.silly.nested;

import org.immutables.annotation.GenerateMarshaler;
import org.immutables.value.Value;

@Value.Nested
@Value.Immutable
public interface InnerNested {
  @Value.Immutable
  @GenerateMarshaler
  abstract static class Inner {}

  @Value.Immutable
  @GenerateMarshaler
  interface Nested {}
}
