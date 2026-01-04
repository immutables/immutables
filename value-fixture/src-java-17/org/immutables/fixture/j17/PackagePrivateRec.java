package org.immutables.fixture.j17;

import jakarta.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(
    jakarta = true,
    jdkOnly = true,
    visibility = Value.Style.ImplementationVisibility.PUBLIC)
@Value.Builder
record PackagePrivateRec(@Nullable String firstName, String lastName) {}
