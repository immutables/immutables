package org.immutables.fixture.style;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(alwaysPublicInitializers = false)
public abstract class NonPublicInitializers {
  public abstract int a();

  protected abstract String b();

  abstract boolean c();
}
