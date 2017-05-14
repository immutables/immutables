package org.immutables.fixture.generics;

import org.immutables.value.Value;

@Value.Immutable
public interface RecursiveGenerics<T extends RecursiveGenerics<T>> {}
