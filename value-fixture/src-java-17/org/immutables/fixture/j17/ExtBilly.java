package org.immutables.fixture.j17;

import org.immutables.value.Value;

@Value.Style(
    isSetOnBuilder = true,
    isSet = "is*Set",
    builderVisibility = Value.Style.BuilderVisibility.PACKAGE)
@Value.Builder
public record ExtBilly(int a, String b, boolean c) {

  static class Builder extends ExtBillyBuilder {
    Builder() {
      b("DefaultB");
      c(false);
    }

    @Override public ExtBilly build() {
      if (!isASet()) {
        a(14);
      }
      return super.build();
    }
  }

  static void use() {
    new ExtBilly.Builder()
        .a(11)
        .b("ABC")
        .build();
  }
}
