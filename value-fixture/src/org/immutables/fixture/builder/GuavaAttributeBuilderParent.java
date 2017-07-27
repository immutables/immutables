package org.immutables.fixture.builder;

import com.google.common.collect.ImmutableList;
import org.immutables.fixture.builder.attribute_builders.FirstPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutable;
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
public abstract class GuavaAttributeBuilderParent implements AttributeBuilderValueI {
    @Override
    public abstract ImmutableList<FirstPartyImmutable> firstPartyImmutableList();

    @Override
    public abstract ImmutableList<ThirdPartyImmutable> thirdPartyImmutableList();

    public static class Builder extends ImmutableGuavaAttributeBuilderParent.Builder implements
        AttributeBuilderBuilderI<GuavaAttributeBuilderParent> {
      public Builder() {
      }
    }
}
