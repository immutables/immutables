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
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Constant;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.ExpressionVisitor;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Visitors;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class GeodeQueryVisitor implements ExpressionVisitor<String> {

  GeodeQueryVisitor() {
  }

  @Override
  public String visit(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());

      final Path path = Visitors.maybePath(args.get(0)).orElseThrow(() -> new IllegalArgumentException("Expected path. Got : " + args.get(0)));
      final Constant constant = Visitors.maybeConstant(args.get(1)).orElseThrow(() -> new IllegalArgumentException("Expected constant. Got " + args.get(1)));
      return String.format("%s = %s", path.toStringPath(), toString(constant.value()));
    }

    if (op == Operators.AND || op == Operators.OR) {
      Preconditions.checkArgument(!args.isEmpty(), "Size should be >=1 for %s but was %s", op, args.size());

      return args.stream().map(a -> a.accept(this)).collect(Collectors.joining(op.name()));
    }

    if (op == Operators.IN || op == Operators.NOT_IN) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final Path field = Visitors.maybePath(args.get(0)).orElseThrow(() -> new IllegalArgumentException("Expected path. Got : " + args.get(0)));
      @SuppressWarnings("unchecked")
      final Iterable<Object> values = (Iterable<Object>) Visitors.maybeConstant(args.get(1))
              .orElseThrow(() -> new IllegalArgumentException("Expected constant. Got " + args.get(1))).value();


      String valuesAsString = StreamSupport.stream(values.spliterator(), false)
              .map(GeodeQueryVisitor::toString).collect(Collectors.joining(", "));

      String query = String.format("%s in SET(%s)", field.toStringPath(), valuesAsString);

      if (op == Operators.NOT_IN) {
        query = "NOT " + query;
      }

      return query;
    }

    throw new UnsupportedOperationException("Don't know how to handle " + call);
  }

  @Override
  public String visit(Constant constant) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String visit(Path path) {
    throw new UnsupportedOperationException();
  }

  private static String toString(Object value) {
    if (value == null) {
      return "null";
    } else if (value instanceof String) {
      return "'" + value + "'";
    } else {
      return Objects.toString(value);
    }
  }

}
