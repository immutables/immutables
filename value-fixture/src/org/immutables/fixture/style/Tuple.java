package org.immutables.fixture.style;

import org.immutables.value.Value;

@Value.Style(
    typeImmutable = "*Tuple",
    allParameters = true,
    defaults = @Value.Immutable(builder = false))
public @interface Tuple {}

@Tuple
@Value.Immutable
interface Color {
  int red();
  int green();
  int blue();
  
  default void use() {
    ColorTuple.of(0xFF, 0x00, 0xFE);
  }
}

@Tuple
@Value.Immutable
interface OverrideColor {
  @Value.Parameter
  int white();

  @Value.Parameter
  int black();

  @Value.Default
  default int gray() {
    return black() - gray();
  }

  default void use() {
    OverrideColorTuple.of(0xFF, 0x00);
  }
}