package org.immutables.trees.ast;

import com.google.common.base.Optional;
import java.util.List;
import org.immutables.trees.Trees;
import org.immutables.value.Value;

/**
 * Compilation test for Ast Tree generation.
 */
@Trees.Ast
@Trees.Transform(include = IncludedTree.class)
@Value.Enclosing
public class SampleTree {
  interface Expression {}

  interface Term extends Expression {}

  @Value.Immutable
  interface Operator extends Expression {
    @Value.Parameter
    Term left();

    @Value.Parameter
    Term right();

    @Value.Parameter
    Kind operator();

    List<Integer> cardinalities();

    Optional<String> position();

    enum Kind {
      PLUS,
      MINUS
    }
  }

  @Value.Immutable
  interface Identifier extends Term {
    @Value.Parameter
    String name();
  }

  @Value.Immutable(singleton = true, builder = false)
  interface Eof {}

  void use() {

  }
}
