package org.immutables.fixture.builder;

import org.immutables.value.Value;

public interface ToBuilderMethod {
  @Value.Immutable
  @Value.Style(toBuilder = "toBuilder")
  interface ToBuilderClassic {
    int a();
    String b();
  }

  @Value.Immutable
  @Value.Style(toBuilder = "toBuilder", overshadowImplementation = true)
  interface ToBuilderSandwich {
    int a();
    String b();
    // abstract to builder is not an accessor if toBuilder enabled
    Builder toBuilder();

    class Builder extends ImmutableToBuilderSandwich.Builder {}
  }
}
