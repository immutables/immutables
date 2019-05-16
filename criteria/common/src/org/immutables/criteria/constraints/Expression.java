package org.immutables.criteria.constraints;

import javax.annotation.Nullable;

/**
 * Expression is a <a href="https://cs.lmu.edu/~ray/notes/ir/">Intermediate Representation</a> (IR)
 * generated by criteria DSL (front-end in compiler terminology).
 * <p>
 * Usually, it is represented as <a href="https://en.wikipedia.org/wiki/Abstract_syntax_tree">AST</a>.
 */
public interface Expression {

  @Nullable
  <R, C> R accept(ExpressionBiVisitor<R, C> visitor, @Nullable C context);

  @Nullable
  default <R> R accept(ExpressionVisitor<R> visitor) {
    return accept(Expressions.toBiVisitor(visitor), null);
  }

}
