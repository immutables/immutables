package org.immutables.fixture.nested;

import org.immutables.value.Value;

public abstract class NonGrouped {

  @Value.Immutable
  abstract static class Abra {}

  @Value.Immutable
  interface Cadabra {

  }
}
