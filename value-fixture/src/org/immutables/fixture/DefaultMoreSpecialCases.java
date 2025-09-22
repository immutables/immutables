package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Style(unsafeDefaultAndDerived = true)
@Value.Immutable
public interface DefaultMoreSpecialCases {
  @Value.Parameter
  int param();

  // When there's only one derived or constant
  @Value.Default.String("Q")
  String constDefault();

  @Value.Derived
  default int derivint() {return 1;}
}
