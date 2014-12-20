package org.immutables.fixture.ast;

import org.immutables.value.Parboil;
import org.immutables.value.Value;

/**
 * Compilation test for Ast Tree generation
 */
@Parboil.Ast
@Value.Nested
public class AstTree {
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
