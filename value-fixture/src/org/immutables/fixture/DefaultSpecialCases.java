package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
public interface DefaultSpecialCases {
  @Value.Parameter
  int param();

  // When there's only one derived or constant
  @Value.Default.String("Q")
  String constDefault();

  @Value.Immutable
  interface Couple {
    @Value.Parameter
    int param();

    @Value.Default.String("Q")
    String constDefault();

    @Value.Default
    default int defavint() {
      return 34;
    }

    @Value.Derived
    default int derivint() {
      return 42;
    }
  }
}
