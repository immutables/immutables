package org.immutables.criteria.expression;

/**
 * Visitor pattern for traversing a tree of expressions.
 *
 * Consider using {@link ExpressionBiVisitor} if you need to propagate some context / payload.
 *
 * @param <V> visitor return type
 */
public interface ExpressionVisitor<V> {

  V visit(Call call);

  V visit(Constant constant);

  V visit(Path path);

}
