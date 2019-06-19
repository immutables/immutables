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

package org.immutables.criteria.geode;

import com.google.common.base.Preconditions;
import org.immutables.criteria.expression.AbstractExpressionVisitor;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Constant;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.OperatorTables;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Visitors;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class GeodeQueryVisitor extends AbstractExpressionVisitor<String> {

  private final Function<Path, String> pathFn;

  GeodeQueryVisitor() {
    this(Path::toStringPath);
  }

  GeodeQueryVisitor(Function<Path, String> pathFn) {
    super(e -> { throw new UnsupportedOperationException(); });
    this.pathFn = Objects.requireNonNull(pathFn, "pathFn");
  }

  @Override
  public String visit(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());

      final Path path = Visitors.toPath(args.get(0));
      final Constant constant = Visitors.toConstant(args.get(1));
      return String.format("%s = %s", pathFn.apply(path), toString(constant.value()));
    }

    if (op == Operators.AND || op == Operators.OR) {
      Preconditions.checkArgument(!args.isEmpty(), "Size should be >=1 for %s but was %s", op, args.size());
      final String join = " " + op.name() + " ";
      return args.stream().map(a -> a.accept(this)).collect(Collectors.joining(join));
    }

    if (op == Operators.IN || op == Operators.NOT_IN) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final Path field = Visitors.toPath(args.get(0));
      final List<Object> values = Visitors.toConstant(args.get(1)).values();

      final String valuesAsString = values.stream()
              .map(GeodeQueryVisitor::toString).collect(Collectors.joining(", "));

      final String query = String.format("%s in SET(%s)", pathFn.apply(field), valuesAsString);

      return op == Operators.NOT_IN ? "NOT " + query : query;
    }

    if (op == Operators.NOT) {
      Preconditions.checkArgument(args.size() == 1, "Size should be 1 for %s but was %s", op, args.size());
      return "NOT " + args.get(0).accept(this);
    }

    if (OperatorTables.COMPARISON.contains(op)) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String fieldAsString = pathFn.apply(Visitors.toPath(args.get(0)));
      final String valueAsString = toString(Visitors.toConstant(args.get(1)).value());

      if (op == Operators.GREATER_THAN) {
        return String.format("%s > %s", fieldAsString, valueAsString);
      } else if (op == Operators.GREATER_THAN_OR_EQUAL) {
        return String.format("%s >= %s", fieldAsString, valueAsString);
      } else if (op == Operators.LESS_THAN) {
        return String.format("%s < %s", fieldAsString, valueAsString);
      } else if (op == Operators.LESS_THAN_OR_EQUAL) {
        return String.format("%s <= %s", fieldAsString, valueAsString);
      }

      throw new UnsupportedOperationException("Unknown comparison " + call);
    }


    throw new UnsupportedOperationException("Don't know how to handle " + call);
  }

  private static String toString(Object value) {
    if (value instanceof CharSequence) {
      return "'" + value + "'";
    } else {
      return Objects.toString(value);
    }
  }

}
