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

import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

/**
 * Simple implementation of {@link Call}
 */
class SimpleCall implements Call {

  private final List<Expression> arguments;
  private final Operator operator;

  SimpleCall(List<Expression> arguments, Operator operator) {
    this.arguments = ImmutableList.copyOf(arguments);
    this.operator = Objects.requireNonNull(operator, "operator");
  }

  @Override
  public List<Expression> arguments() {
    return arguments;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Nullable
  @Override
  public <R, C> R accept(ExpressionBiVisitor<R, C> visitor, @Nullable C context) {
    return visitor.visit(this, context);
  }

  @Override
  public Type returnType() {
    return operator.returnType();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SimpleCall that = (SimpleCall) o;
    return Objects.equals(arguments, that.arguments) &&
            Objects.equals(operator, that.operator);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arguments, operator);
  }

  @Override
  public String toString() {
    final StringWriter writer = new StringWriter();
    final PrintWriter printer = new PrintWriter(writer);
    printer.append(SimpleCall.class.getSimpleName());
    printer.append("{");
    printer.print(operator().name());
    DebugExpressionVisitor<PrintWriter> debug = new DebugExpressionVisitor<>(printer);
    arguments().forEach(a -> {
      a.accept(debug);
    });
    printer.append("}");
    return writer.toString();
  }
}
