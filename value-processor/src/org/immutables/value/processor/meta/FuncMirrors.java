package org.immutables.value.processor.meta;

import org.immutables.mirror.Mirror;

public final class FuncMirrors {
  private FuncMirrors() {}

  @Mirror.Annotation("org.immutables.func.Functional")
  public @interface Functional {}

}
