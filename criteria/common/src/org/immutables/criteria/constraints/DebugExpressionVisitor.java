package org.immutables.criteria.constraints;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Objects;

/**
 * Used to output expression tree as string. Useful for debugging expressions.
 */
public class DebugExpressionVisitor<Void> implements ExpressionVisitor<Void> {

  private final PrintWriter writer;

  private int depth;

  public DebugExpressionVisitor(PrintWriter writer) {
    this.writer = Objects.requireNonNull(writer, "writer");
  }

  @Override
  public Void visit(Call<?> call) {
    writer.println();
    writer.print(String.join("", Collections.nCopies(depth * 2, " ")));
    writer.print("call op=" + call.getOperator());
    for (Expression<?> expr: call.getArguments()) {
      depth++;
      expr.accept(this);
      depth--;
    }
    return null;
  }

  @Override
  public Void visit(Literal<?> literal) {
    writer.print(" literal=");
    writer.print(literal.value());
    return null;
  }

  @Override
  public Void visit(Path<?> path) {
    writer.print(" path=");
    writer.print(path.path());
    return null;
  }
}
