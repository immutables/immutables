package org.immutables.gson.packg;

import org.immutables.value.Value;

@Value.Immutable
public interface B {
  @Value.Immutable(builder = false)
  public interface C {}
}
