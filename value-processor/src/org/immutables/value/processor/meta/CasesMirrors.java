package org.immutables.value.processor.meta;

import org.immutables.mirror.Mirror;

public final class CasesMirrors {
  private CasesMirrors() {}

  @Mirror.Annotation("org.immutables.cases.Cases.Chain")
  public @interface Chain {}
}
