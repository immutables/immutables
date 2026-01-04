package org.immutables.fixture.j17;

import jakarta.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(
    jakarta = true,
    jdkOnly = true,
    visibility = Value.Style.ImplementationVisibility.PUBLIC,
    mergeFromSupertypesDynamically = false
)
@Value.Builder
record PackagePrivateStaticallyFrom(
    @Nullable String firstName,
    String lastName) implements HasLastName {}
