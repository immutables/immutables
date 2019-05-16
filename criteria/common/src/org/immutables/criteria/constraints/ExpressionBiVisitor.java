package org.immutables.criteria.constraints;

import javax.annotation.Nullable;

/**
 * Visitor which also accepts a payload.
 *
 * @param <V> visitor return type
 * @param <C> context type
 */
public interface ExpressionBiVisitor<V, C> {

  V visit(Call call, @Nullable C context);

  V visit(Literal literal, @Nullable C context);

  V visit(Path path, @Nullable C context);

}
