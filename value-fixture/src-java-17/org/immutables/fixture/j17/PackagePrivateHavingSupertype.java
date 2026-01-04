package org.immutables.fixture.j17;

import jakarta.annotation.Nullable;
import org.immutables.value.Value;

interface HasLastName {
  String lastName();
}

@Value.Style(
    jakarta = true,
    jdkOnly = true,
    visibility = Value.Style.ImplementationVisibility.PUBLIC)
@Value.Builder
record PackagePrivateHavingSupertype(
    @Nullable String firstName,
    String lastName) implements HasLastName {}
