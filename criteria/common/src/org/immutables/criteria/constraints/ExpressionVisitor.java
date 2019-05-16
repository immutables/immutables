package org.immutables.criteria.constraints;

/**
 * Visitor pattern for traversing a tree of expressions.
 *
 * Consider using {@link ExpressionBiVisitor} if you need to propagate some context / payload.
 *
 * @param <V> visitor return type
 */
public interface ExpressionVisitor<V> {

  V visit(Call call);

  V visit(Literal literal);

  V visit(Path path);

}
