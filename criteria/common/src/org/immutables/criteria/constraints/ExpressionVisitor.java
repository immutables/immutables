package org.immutables.criteria.constraints;

import javax.annotation.Nullable;

/**
 *
 * @param <V> visitor return type
 * @param <C> context type
 */
public interface ExpressionVisitor<V, C> {

  V visit(Call<?> call, @Nullable C context);

  V visit(Literal<?> literal, @Nullable C context);

  V visit(Path<?> path, @Nullable C context);

}
