package org.immutables.fixture.builder.attribute_builders;

import org.immutables.value.Value.Immutable;

@Immutable
public abstract class FirstPartyWithBuilderExtension {
  public abstract String value();

  public static class Builder extends ImmutableFirstPartyWithBuilderExtension.Builder {
  }
}
