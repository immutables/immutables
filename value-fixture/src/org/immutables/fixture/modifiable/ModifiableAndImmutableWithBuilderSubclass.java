package org.immutables.fixture.modifiable;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Modifiable;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.BuilderVisibility;

@Immutable
@Modifiable
@Style(builderVisibility = BuilderVisibility.PACKAGE)
public interface ModifiableAndImmutableWithBuilderSubclass {
  class Builder extends ImmutableModifiableAndImmutableWithBuilderSubclass.Builder {}

  String getId();
}
