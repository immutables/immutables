package org.immutables.value.processor.meta;

import org.immutables.mirror.Mirror;

public final class BuilderMirrors {
  private BuilderMirrors() {}

  @Mirror.Annotation("org.immutables.value.Builder.Factory")
  public @interface Factory {}
}
