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
import com.google.common.primitives.Primitives;

import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.AbstractExpressionVisitor;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.ComparableOperators;
import org.immutables.criteria.expression.Constant;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.IterableOperators;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.OptionalOperators;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.StringOperators;
import org.immutables.criteria.expression.Visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generates <a href="https://geode.apache.org/docs/guide/16/developing/querying_basics/query_basics.html">Geode OQL</a>
 * based on existing expression.
 */
class GeodeQueryVisitor extends AbstractExpressionVisitor<Oql> {

  private final PathNaming pathNaming;

  /**
   * Bind variables. Remains empty if variables are not used
   */
  private final List<Object> variables;

  private final boolean useBindVariables;

  /**
   * @param useBindVariables wherever query should be generated with bind variables or not
   */
  GeodeQueryVisitor(boolean useBindVariables, PathNaming pathNaming) {
    super(e -> { throw new UnsupportedOperationException(); });
    this.pathNaming = Objects.requireNonNull(pathNaming, "pathFn");
    this.variables = new ArrayList<>();
    this.useBindVariables = useBindVariables;
  }

  @Override
  public Oql visit(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    if (op == Operators.NOT_IN || op == IterableOperators.NOT_EMPTY) {
      // geode doesn't understand syntax foo not in [1, 2, 3]
      // convert "foo not in [1, 2, 3]" into "not foo in [1, 2, 3]"
      return visit(Expressions.not(Expressions.call(getInverseOp(op), call.arguments())));
    }

    if (op == Operators.AND || op == Operators.OR) {
      Preconditions.checkArgument(!args.isEmpty(), "Size should be >=1 for %s but was %s", op, args.size());
      final String join = ") " + op.name() + " (";
      final String newOql = "(" + args.stream().map(a -> a.accept(this)).map(Oql::oql).collect(Collectors.joining(join)) + ")";
      return new Oql(variables, newOql);
    }

    if (op.arity() == Operator.Arity.BINARY) {
      return binaryOperator(call);
    }

    if (op.arity() == Operator.Arity.UNARY) {
      return unaryOperator(call);
    }

    throw new UnsupportedOperationException("Don't know how to handle " + call);
  }

  private static Operator getInverseOp(Operator op) {
    if (op == Operators.NOT_IN) {
      return Operators.IN;
    } else if (op == IterableOperators.NOT_EMPTY) {
      return IterableOperators.IS_EMPTY;
    } else {
      throw new IllegalArgumentException("Don't know inverse operator of " + op);
    }
  }

  /**
   * Operator with single operator: {@code NOT}, {@code IS_PRESENT}
   */
  private Oql unaryOperator(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    Preconditions.checkArgument(args.size() == 1,
            "Size should be == 1 for unary operator %s but was %s", op, args.size());

    if (op instanceof OptionalOperators) {
      final Path path = Visitors.toPath(args.get(0));
      final String isNull = op == OptionalOperators.IS_PRESENT ? "!= null" : "= null";
      return oql(pathNaming.name(path) + " " + isNull);
    } else if (op == Operators.NOT) {
      return oql("NOT (" + args.get(0).accept(this).oql() + ")");
    }  else if (op == IterableOperators.IS_EMPTY) {
      final Path path = Visitors.toPath(args.get(0));
      return oql(pathNaming.name(path) + "." + toMethod(op));
    }

    throw new UnsupportedOperationException("Unknown unary operator " + call);
  }

  /**
   * Used for operators with two arguments like {@code =}, {@code IN} etc.
   */
  private Oql binaryOperator(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();
    Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s on call %s", op, args.size(), call);

    final String namedPath = getNamedPath(call);

    final Object variable = getVariable(call);

    if (op == IterableOperators.CONTAINS) {
      Preconditions.checkArgument(
              useBindVariables || isStringOrPrimitive(variable),
              "String or Primitive types only supported for contains operator but was " + variable.getClass()
      );
      return oql(String.format("%s.contains(%s)", namedPath, addAndGetBoundVariable(variable)));
    } else if (op == StringOperators.MATCHES) {
      return oql(String.format("%s.matches(%s)", namedPath, addAndGetBoundVariable(variable)));
    } else if (op == StringOperators.CONTAINS || op == StringOperators.STARTS_WITH || op == StringOperators.ENDS_WITH) {
      Preconditions.checkArgument(variable instanceof CharSequence, "Variable should be String but was " + variable);

      final String value = Geodes.escapeOql((CharSequence) variable);
      final String likePattern = getLikePattern(op, value);
      if (useBindVariables) {
        variables.add(likePattern);
        return oql(String.format("%s LIKE $%d", namedPath, variables.size()));
      } else {
        return oql(String.format("%s LIKE '%s'", namedPath, likePattern));
      }
    }

    final String operator;
    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
      operator = op == Operators.EQUAL ? "=" : "!=";
    } else if (op == Operators.IN || op == Operators.NOT_IN) {
      operator = op == Operators.IN ? "IN" : "NOT IN";
    } else if (op == ComparableOperators.GREATER_THAN) {
      operator = ">";
    } else if (op == ComparableOperators.GREATER_THAN_OR_EQUAL) {
      operator = ">=";
    } else if (op == ComparableOperators.LESS_THAN) {
      operator = "<";
    } else if (op == ComparableOperators.LESS_THAN_OR_EQUAL) {
      operator = "<=";
    } else if (op == IterableOperators.HAS_SIZE || op == StringOperators.HAS_LENGTH) {
      operator = "=";
    } else {
      throw new IllegalArgumentException("Unknown binary operator " + call);
    }

    return oql(String.format("%s %s %s", namedPath, operator, addAndGetBoundVariable(variable)));
  }

  private static boolean isStringOrPrimitive(Object variable) {
    return variable instanceof CharSequence || Primitives.isWrapperType(Primitives.wrap(variable.getClass()));
  }

  private static String getLikePattern(Operator op, String value) {
    if (op == StringOperators.STARTS_WITH) {
      return value + "%";
    } else if (op == StringOperators.ENDS_WITH) {
      return "%" + value;
    } else if (op == StringOperators.CONTAINS) {
      return "%" + value + "%";
    } else {
      throw new IllegalArgumentException("LIKE query used with invalid operator " + op);
    }
  }

  private String getNamedPath(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    final Path path = Visitors.toPath(args.get(0));
    String namedPath = pathNaming.name(path);
    if (op == IterableOperators.HAS_SIZE) {
      return namedPath + ".size";
    } else if (op == StringOperators.HAS_LENGTH) {
      return namedPath + ".length";
    }
    return namedPath;
  }

  private static Object getVariable(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    final Constant constant = Visitors.toConstant(args.get(1));

    if (op == Operators.IN || op == Operators.NOT_IN) {
      return ImmutableSet.copyOf(constant.values());
    } else {
      return constant.value();
    }
  }

  private String addAndGetBoundVariable(Object variable) {
    return maybeAddAndGetBoundVariable(variable)
            .orElseGet(() -> toString(variable));
  }

  private Optional<String> maybeAddAndGetBoundVariable(Object variable) {
    if (useBindVariables) {
      variables.add(variable);

      return Optional.of("$" + String.valueOf(variables.size()));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Return new query but with same variables
   */
  private Oql oql(String oql) {
    return new Oql(variables, oql);
  }

  private static String toMethod(Operator op) {
    if (op == IterableOperators.IS_EMPTY) {
      return "isEmpty";
    }
    throw new UnsupportedOperationException("Don't know how to handle Operator " + op);
  }

  private static String toString(Object value) {
    if (value instanceof CharSequence) {
      return "'" + Geodes.escapeOql((CharSequence) value) + "'";
    } else if (value instanceof Pattern) {
      return "'" + Geodes.escapeOql(((Pattern) value).pattern()) + "'";
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
