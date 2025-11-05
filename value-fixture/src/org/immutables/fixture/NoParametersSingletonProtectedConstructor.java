package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable(singleton = true)
@Value.Style(
    allParameters = true,
    protectedNoargConstructor = true
)
public interface NoParametersSingletonProtectedConstructor {}
