package org.immutables.fixture.with;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(withUnaryOperator = "map*")
public interface WithMapUnary extends WithUnaryOperator, WithWithMapUnary {
	// inherit all attributes from WithUnaryOperator and generate With-interface
	class Builder extends ImmutableWithMapUnary.Builder {}
}
