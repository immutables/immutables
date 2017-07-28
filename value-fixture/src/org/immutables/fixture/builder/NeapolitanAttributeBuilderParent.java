package org.immutables.fixture.builder;

import org.immutables.fixture.builder.attribute_builders.FirstPartyWithBuilderExtension;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithBuilderClassCopyMethod;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithBuilderInstanceCopyMethod;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithNestedBuilder;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithValueClassCopyMethod;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithValueInstanceCopyMethod;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(
    attributeBuilderDetection = true
)
public abstract class NeapolitanAttributeBuilderParent {
  public abstract ThirdPartyImmutableWithValueInstanceCopyMethod tpiWithValueInstanceCopyMethod();

  public abstract ThirdPartyImmutableWithValueClassCopyMethod tpiWithValueClassCopyMethod();

  public abstract ThirdPartyImmutableWithBuilderInstanceCopyMethod tpiWithBuilderInstanceCopyMethod();

  public abstract ThirdPartyImmutableWithBuilderClassCopyMethod tpiWithBuilderClassCopyMethod();

  public abstract FirstPartyWithBuilderExtension fpWithBuilderExtension();

  public abstract ThirdPartyImmutableWithNestedBuilder tpiWithNestedBuilder();
}
