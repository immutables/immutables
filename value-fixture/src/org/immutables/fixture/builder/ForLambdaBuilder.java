package org.immutables.fixture.builder;

import org.immutables.fixture.builder.attribute_builders.FirstPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(attributeBuilderDetection = true)
public interface ForLambdaBuilder {
	FirstPartyImmutable firstPartyImmutable();
	ThirdPartyImmutable thirdPartyImmutable();
}
