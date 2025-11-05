package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
    allParameters = true,
    protectedNoargConstructor = true
)
public interface NoParametersProtectedConstructor {}
