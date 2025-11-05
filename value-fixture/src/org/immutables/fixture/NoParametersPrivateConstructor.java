package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
    allParameters = true,
    privateNoargConstructor = true
)
public interface NoParametersPrivateConstructor {}
