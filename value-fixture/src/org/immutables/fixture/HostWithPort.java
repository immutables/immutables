package org.immutables.fixture;

import org.immutables.value.Value;

// Compilation test for costructor parameters order
// also runtime checks for null
// regressions #148, #149
@Value.Immutable
public abstract class HostWithPort {
  @Value.Parameter(order = 1)
  public abstract String hostname();
  @Value.Parameter
  public abstract int port();
}