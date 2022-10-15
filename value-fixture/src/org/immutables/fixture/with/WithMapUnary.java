package org.immutables.fixture.with;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(withUnaryOperator = "map*")
public interface WithMapUnary extends WithUnaryOperator, WithWithMapUnary {
  // inherit all attributes from WithUnaryOperator and generate With-interface
  class Builder extends ImmutableWithMapUnary.Builder {}

  default void compiledAndUsable() {
    WithMapUnary u = new Builder()
        .a(1)
        .b("B")
        .build();

    // signatures exist after code generation and compilation
    u = u.mapA(a -> a + 2).mapL(l -> l + "!!!").mapO(o -> o + "???");
  }
}
