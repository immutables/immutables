package org.immutables.fixture;

import org.immutables.value.Value.Style.ImplementationVisibility;
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

@Value.Style(typeBuilder = "*_Builder", visibility = ImplementationVisibility.PRIVATE)
@Value.Immutable
class ExtendingInnerBuilderValue {

  static class Builder extends ExtendingInnerBuilderValue_Builder {}

  static void use() {
    new ExtendingInnerBuilderValue.Builder().build();
  }
}
