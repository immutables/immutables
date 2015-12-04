package org.immutables.fixture.style;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
    implementationNestedInBuilder = true,
    builderVisibility = Value.Style.BuilderVisibility.PACKAGE)
public interface AInterface {
  String getString();
}
