package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
class SuperInnerBuildeValue {
  static class Builder {

    void hello() {}
  }

  static void use() {
    ImmutableSuperInnerBuildeValue.builder().hello();
  }
}

@Value.Immutable
class ExtendingInnerBuilderValue {

  static class Builder extends ImmutableExtendingInnerBuilderValue.Builder {}

  static void use() {
    new ExtendingInnerBuilderValue.Builder().build();
  }
}
