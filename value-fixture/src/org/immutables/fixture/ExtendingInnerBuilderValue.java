package org.immutables.fixture;

import java.util.List;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

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
@Value.Style(
    typeBuilder = "*_Builder",
    visibility = ImplementationVisibility.PRIVATE)
abstract class ExtendingInnerBuilderValue {

  abstract int attribute();

  abstract List<String> list();

  static class Builder extends ExtendingInnerBuilderValue_Builder {
    Builder() {
      attribute(1);
    }
  }
}

@Value.Immutable
@Value.Style(
    typeInnerBuilder = "Creator",
    typeBuilder = "Creator",
    build = "create",
    visibility = ImplementationVisibility.SAME_NON_RETURNED)
interface ExtendingInnerCreatorValue {

  static class Creator extends ImmutableExtendingInnerCreatorValue.Creator {
    @Override
    public ExtendingInnerCreatorValue create() {
      return super.create();
    }
  }

  default void use() {
    ImmutableExtendingInnerCreatorValue.Creator c = new ImmutableExtendingInnerCreatorValue.Creator();
    c.create();
  }
}
