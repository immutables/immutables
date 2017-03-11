package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(allMandatoryParameters = true, defaultAsDefault = true)
public interface AllMandatoryParams {
  int a();

  boolean b();

  default String c() {
    return "C";
  }

  static void use() {
    ImmutableAllMandatoryParams.of(1, true).withC("ABC");
  }
}
