package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
interface ExtendedBuilderInterface {
  int a();

  int b();

  interface Builder {
    Builder a(int a);

    Builder b(int b);

    ExtendedBuilderInterface build();
  }

  static Builder builder() {
    return ImmutableExtendedBuilderInterface.builder();
  }
}
