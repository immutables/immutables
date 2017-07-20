package org.immutables.fixture.builder.attribute_builders;

import org.immutables.builder.Builder.AccessibleFields;
import org.immutables.value.Value.Immutable;

@Immutable
public abstract class FirstPartyWithBuilderExtension {
  public static final String EXTENSION_OVERRIDE = "Extension Override: ";

  public abstract String value();

  @AccessibleFields
  public static class Builder extends ImmutableFirstPartyWithBuilderExtension.Builder {

    @Override
    public ImmutableFirstPartyWithBuilderExtension build() {
      this.value = EXTENSION_OVERRIDE.concat(value);
      return super.build();
    }
  }
}
