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

import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

/**
 * An expression formed by a call to an operator (eg. {@link Operators#EQUAL}) with zero or more arguments.
 */
@Value.Immutable(lazyhash = true)
@Value.Style(visibility = Value.Style.ImplementationVisibility.PACKAGE)
public abstract class Call implements Expression {

  Call() {}

  /**
   * Get arguments of this operation
   */
  @Value.Parameter
  public abstract List<Expression> arguments();

  /**
   * Get the operator symbol for this operation
   *
   * @return operator
   */
  @Value.Parameter
  public abstract Operator operator();

  @Value.Parameter
  @Override
  public abstract Type returnType();

  @Nullable
  @Override
  public <R, C> R accept(ExpressionBiVisitor<R, C> visitor, @Nullable C context) {
    return visitor.visit(this, context);
  }

  static Call of(Operator operator, Iterable<? extends Expression> arguments) {
    Objects.requireNonNull(operator, "operator");
    return ImmutableCall.of(arguments, operator, operator.returnType());
  }

  @Override
  public String toString() {
    final StringWriter writer = new StringWriter();
    final PrintWriter printer = new PrintWriter(writer);
    printer.append(getClass().getSimpleName());
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
