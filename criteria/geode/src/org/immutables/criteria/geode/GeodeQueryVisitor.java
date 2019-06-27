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
import com.google.common.collect.ImmutableSet;
import org.immutables.criteria.expression.AbstractExpressionVisitor;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Constant;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.OperatorTables;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

class GeodeQueryVisitor extends AbstractExpressionVisitor<OqlWithVariables> {

  private final Function<Path, String> pathFn;

  /**
   * Bind variables
   */
  private final List<Object> variables;

  private final boolean useBindVariables;

  GeodeQueryVisitor(boolean useBindVariables) {
    this(useBindVariables, Path::toStringPath);
  }

  GeodeQueryVisitor(boolean useBindVariables, Function<Path, String> pathFn) {
    super(e -> { throw new UnsupportedOperationException(); });
    this.pathFn = Objects.requireNonNull(pathFn, "pathFn");
    this.variables = new ArrayList<>();
    this.useBindVariables = useBindVariables;
  }


  @Override
  public OqlWithVariables visit(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL ||
        op == Operators.IN || op == Operators.NOT_IN || OperatorTables.COMPARISON.contains(op)) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      return binaryOperator(call);
    }

    if (op == Operators.AND || op == Operators.OR) {
      Preconditions.checkArgument(!args.isEmpty(), "Size should be >=1 for %s but was %s", op, args.size());
      final String join = " " + op.name() + " ";
      final String newOql = args.stream().map(a -> a.accept(this)).map(OqlWithVariables::oql).collect(Collectors.joining(join));
      return new OqlWithVariables(variables, newOql);
    }

    if (op == Operators.NOT) {
      Preconditions.checkArgument(args.size() == 1, "Size should be 1 for %s but was %s", op, args.size());
      return oql("NOT " + args.get(0).accept(this).oql());
    }

    throw new UnsupportedOperationException("Don't know how to handle " + call);
  }

  private OqlWithVariables binaryOperator(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();
    Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s on call %s", op, args.size(), call);
    final String operator;
    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
      operator = op == Operators.EQUAL ? "=" : "!=";
    } else if (op == Operators.IN || op == Operators.NOT_IN) {
      operator = op == Operators.IN ? "in" : "not in";
    } else if (op == Operators.GREATER_THAN) {
      operator = ">";
    } else if (op == Operators.GREATER_THAN_OR_EQUAL) {
      operator = ">=";
    } else if (op == Operators.LESS_THAN) {
      operator = "<";
    } else if (op == Operators.LESS_THAN_OR_EQUAL) {
      operator = "<=";
    } else {
      throw new IllegalArgumentException("Unknown binary operator " + call);
    }

    final Path path = Visitors.toPath(args.get(0));
    final Constant constant = Visitors.toConstant(args.get(1));

    final Object variable;
    if (op == Operators.IN || op == Operators.NOT_IN) {
      variable = ImmutableSet.copyOf(constant.values());
    } else {
      variable = constant.value();
    }

    if (useBindVariables) {
      variables.add(variable);
      return oql(String.format("%s %s $%d", pathFn.apply(path), operator, variables.size()));
    }

    return oql(String.format("%s %s %s", pathFn.apply(path), operator, toString(variable)));
  }

  /**
   * Return new query but with same variables
   */
  private OqlWithVariables oql(String oql) {
    return new OqlWithVariables(variables, oql);
  }

  private static String toString(Object value) {
    if (value instanceof CharSequence) {
      return "'" + value + "'";
    } else if (value instanceof Collection) {
      @SuppressWarnings("unchecked")
      final Collection<Object> coll = (Collection<Object>) value;
      String asString = coll.stream().map(GeodeQueryVisitor::toString).collect(Collectors.joining(", "));
      return String.format("SET(%s)", asString);
    } else {
      return Objects.toString(value);
    }
  }

}
