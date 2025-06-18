package org.immutables.fixture.nullable.typeuse;

import org.immutables.value.Value;
import org.jspecify.annotations.Nullable;

public interface TypeUseFromSupertypes {
  String string();

  @Nullable ImmutableMyField myField();

  @Value.Immutable
  interface MyField {}

  @Value.Immutable
  abstract class FirstChild implements TypeUseFromSupertypes {}

  @Value.Immutable
  interface SecondChild extends TypeUseFromSupertypes {}
}
