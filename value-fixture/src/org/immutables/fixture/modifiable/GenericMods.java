package org.immutables.fixture.modifiable;

import org.immutables.value.Value;

interface Supertype {
  int a();

  int c();
}

// Compilation test for generics and wildcards when generating
// from supertype in modifiable and immutable generated types
@Value.Immutable
@Value.Modifiable
public interface GenericMods<T> extends Supertype {
  T b();

  @Override
  int c();
}
