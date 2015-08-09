package org.immutables.fixture.modifiable;

import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
interface Companion {
  @Value.Modifiable
  interface Standalone {

  }
}
