package org.immutables.fixture.builder.detection;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(
    attributeBuilderDetection = true,
    attributeBuilder = {"Builder", "*Builder", "builder", "from", "build", "*Build"}
)
public abstract class NoNewTokenAttributeBuilderParent implements NestedDetection {
}
