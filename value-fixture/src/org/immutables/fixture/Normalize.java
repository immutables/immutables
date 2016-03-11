package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
public interface Normalize {
  int value();

  @Value.Check
  default Normalize normalize() {
    if (value() == Integer.MIN_VALUE) {
      return ImmutableNormalize.builder()
          .value(0)
          .build();
    }
    if (value() < 0) {
      return ImmutableNormalize.builder()
          .value(-value())
          .build();
    }
    return this;
  }
}
