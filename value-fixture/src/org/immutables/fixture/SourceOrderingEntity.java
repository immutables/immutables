package org.immutables.fixture;

import org.immutables.value.Value;

interface InheritedInterface {
  int b();
  int a();
}

@Value.Immutable
public abstract class SourceOrderingEntity implements InheritedInterface {

  public abstract int z();

  public abstract int y();
}
