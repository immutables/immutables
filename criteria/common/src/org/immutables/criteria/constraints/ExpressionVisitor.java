package org.immutables.criteria.constraints;

public interface ExpressionVisitor<V> {

  V visit(Call<?> call);

  V visit(Literal<?> literal);

  V visit(Path<?> path);

}
