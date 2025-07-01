package org.immutables.fixture.with;

import org.immutables.value.Value;

@Value.Immutable
public interface AnImm {
  @Value.Parameter
  int a();
}
