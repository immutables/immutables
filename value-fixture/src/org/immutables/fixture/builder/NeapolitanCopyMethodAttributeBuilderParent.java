package org.immutables.fixture.builder;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(
    attributeBuilderDetection = true
)
public abstract class NeapolitanCopyMethodAttributeBuilderParent {
  public abstract ThirdPartyImmutableWithValueInstanceCopyMethod tpiWithValueInstanceCopyMethod();

  public abstract ThirdPartyImmutableWithValueClassCopyMethod tpiWithValueClassCopyMethod();

  public abstract ThirdPartyImmutableWithBuilderInstanceCopyMethod tpiWithBuilderInstanceCopyMethod();

  public abstract ThirdPartyImmutableWithBuilderClassCopyMethod tpiWithBuilderClassCopyMethod();
}
