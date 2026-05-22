package org.immutables.fixture.jdkonly;

import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public interface DerivedDefaultOptional {
  int a();

  @Value.Derived
  default int derived() {
    return a() - b().orElse(0);
  }

  @Value.Default
  default Optional<Integer> b() {
    return Optional.of(5);
  }
}
