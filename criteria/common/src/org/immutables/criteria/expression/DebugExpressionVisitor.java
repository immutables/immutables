package org.immutables.criteria.expression;

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
  public Void visit(Call call) {
    writer.println();
    writer.print(String.join("", Collections.nCopies(depth * 2, " ")));
    writer.print("call op=" + call.operator().name());
    for (Expression expr: call.arguments()) {
      depth++;
      expr.accept(this);
      depth--;
    }
    return null;
  }

  @Override
  public Void visit(Constant constant) {
    writer.print(" constant=");
    writer.print(constant.value());
    return null;
  }

  @Override
  public Void visit(Path path) {
    writer.print(" path=");
    writer.print(path.toStringPath());
    return null;
  }
}
