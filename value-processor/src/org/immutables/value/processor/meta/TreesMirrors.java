package org.immutables.value.processor.meta;

import org.immutables.mirror.Mirror;

public final class TreesMirrors {
  private TreesMirrors() {}

  @Mirror.Annotation("org.immutables.trees.Trees.Ast")
  public @interface Ast {}

  @Mirror.Annotation("org.immutables.trees.Trees.Transform")
  public @interface Transform {
    Class<?>[] include() default {};
  }
}
