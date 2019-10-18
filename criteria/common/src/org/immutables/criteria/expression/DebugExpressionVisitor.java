/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.expression;

import com.google.common.io.CharStreams;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Objects;

/**
 * Used to output expression tree as string. Useful for debugging expressions.
 */
public class DebugExpressionVisitor<A extends Appendable> extends AbstractExpressionVisitor<A> {

  private final PrintWriter writer;
  private final A target;

  private int depth;

  public DebugExpressionVisitor(A target) {
    super(e -> { throw new UnsupportedOperationException(); });
    this.target = Objects.requireNonNull(target, "target");
    this.writer = new PrintWriter(CharStreams.asWriter(target));
  }

  private A target() {
    return target;
  }

  @Override
  public A visit(Call call) {
    writer.println();
    writer.print(String.join("", Collections.nCopies(depth * 2, " ")));
    writer.print("call op=" + call.operator().name());
    for (Expression expr: call.arguments()) {
      depth++;
      expr.accept(this);
      depth--;
    }
    return target();
  }

  @Override
  public A visit(Constant constant) {
    writer.print(" constant=");
    writer.print(constant.value());
    return target();
  }

  @Override
  public A visit(Path path) {
    writer.print(" path=");
    writer.print(path.toStringPath());
    return target();
  }
}
