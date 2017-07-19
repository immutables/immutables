package org.immutables.fixture.builder;

import java.util.List;

public interface AttributeBuilderValueI {
  FirstPartyImmutable firstPartyImmutable();

  FirstPartyImmutableWithDifferentStyle firstPartyImmutableWithDifferentStyle();

  List<FirstPartyImmutable> firstPartyImmutableList();

  ThirdPartyImmutable thirdPartyImmutable();

  List<ThirdPartyImmutable> thirdPartyImmutableList();
}
