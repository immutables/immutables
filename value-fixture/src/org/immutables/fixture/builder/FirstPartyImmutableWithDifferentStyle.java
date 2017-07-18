package org.immutables.fixture.builder;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(
    builder = "getTheBuilder",
    build = "doIIT",
    from = "makeDaCopy",
    typeBuilder = "Abonabon"
)
public abstract class FirstPartyImmutableWithDifferentStyle {

  public abstract String value();
}
