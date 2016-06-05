package org.immutables.trees.ast;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;
import org.immutables.trees.Trees.Transform;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;

@Transform
@Enclosing
@Value.Style(
    visibility = ImplementationVisibility.PACKAGE,
    overshadowImplementation = true)
public interface Wither {

  @Immutable
  interface Suppied extends ImmutableWither.WithSuppied {
    String a();

    long num();

    class Builder extends ImmutableWither.Suppied.Builder {}
  }
}
