package org.immutables.fixture.builder;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(
    attributeBuilderDetection = true,
    depluralize = true,
    depluralizeDictionary = {
        ":list",
    })
public abstract class VanillaAttributeBuilderParent implements AttributeBuilderValueI {
  public static class Builder extends ImmutableVanillaAttributeBuilderParent.Builder implements AttributeBuilderBuilderI<VanillaAttributeBuilderParent> {
    public Builder() {
    }
  }
}
