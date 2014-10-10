package org.immutables.value.sample;

import org.immutables.value.Value;

@Value.Nested
public class SampleNesting {
  @Value.Immutable
  interface A {}

  @Value.Immutable
  public static class B {}
}
