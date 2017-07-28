package org.immutables.fixture.builder;

import org.immutables.fixture.builder.functional.AttributeBuilderBuilderI;
import org.immutables.fixture.builder.functional.AttributeBuilderValueI;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(
    attributeBuilderDetection = true,
    jdkOnly = true,
    depluralize = true,
    depluralizeDictionary = {
        ":list",
    })
public abstract class JdkOnlyAttributeBuilderParent implements AttributeBuilderValueI {

  public static class Builder extends ImmutableJdkOnlyAttributeBuilderParent.Builder implements
      AttributeBuilderBuilderI<JdkOnlyAttributeBuilderParent> {

    public Builder() {
    }
  }
}
