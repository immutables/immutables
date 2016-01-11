package org.immutables.trees.grammar;

import org.immutables.trees.Trees;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Trees.Ast
@Trees.Transform
@Value.Enclosing
@Value.Style(visibility = ImplementationVisibility.PUBLIC, overshadowImplementation = true)
public interface Grammar {

  @Value.Immutable
  public interface StringLiteral {
    @Value.Parameter
    String value();
  }

  @Value.Immutable
  public interface Identifier {
    @Value.Parameter
    String value();
  }
}
