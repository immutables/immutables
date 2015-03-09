package org.immutables.value.processor.meta;

import org.immutables.mirror.Mirror;

public final class BuilderMirrors {
  private BuilderMirrors() {}

  @Mirror.Annotation("org.immutables.builder.Builder.Factory")
  public @interface Factory {}

  @Mirror.Annotation("org.immutables.builder.Builder.Parameter")
  public @interface FParameter {}

  @Mirror.Annotation("org.immutables.builder.Builder.Switch")
  public @interface Switch {
    String defaultName() default "";
  }
}
