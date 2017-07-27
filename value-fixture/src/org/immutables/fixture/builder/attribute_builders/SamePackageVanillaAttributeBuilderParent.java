package org.immutables.fixture.builder.attribute_builders;


import org.immutables.fixture.builder.functional.AttributeBuilderBuilderI;
import org.immutables.fixture.builder.functional.AttributeBuilderValueI;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(
    attributeBuilderDetection = true,
    depluralize = true,
    depluralizeDictionary = {
        ":list",
    })
public abstract class SamePackageVanillaAttributeBuilderParent implements AttributeBuilderValueI {
  public static class Builder extends ImmutableSamePackageVanillaAttributeBuilderParent.Builder implements
      AttributeBuilderBuilderI<SamePackageVanillaAttributeBuilderParent> {
    public Builder() {
    }
  }
}
