package org.immutables.fixture.builder;

import java.util.List;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(attributeBuilderDetection = true, depluralize = true, clearBuilder = true, depluralizeDictionary = {
    ":list",
})
public abstract class AttributeBuilderParent {

  public abstract FirstPartyImmutable firstPartyImmutable();

  public abstract FirstPartyImmutableWithDifferentStyle firstPartyImmutableWithDifferentStyle();

  public abstract List<FirstPartyImmutable> firstPartyImmutableList();

  public abstract ThirdPartyImmutable thirdPartyImmutable();

  public abstract List<ThirdPartyImmutable> thirdPartyImmutableList();

  public abstract ThirdPartyImmutableWithValueInstanceCopyMethod thirdPartyImmutableWithInstanceCopyMethod();
  // TODO: add value instance for builder instantiation on class, and no-arg constructor
}
