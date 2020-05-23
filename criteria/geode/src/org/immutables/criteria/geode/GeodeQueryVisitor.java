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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
      // convert "foo not in [1, 2, 3]" into "not (foo in [1, 2, 3])"
      return visit(Expressions.not(Expressions.call(inverseOp(op), call.arguments())));
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

  private static Operator inverseOp(Operator op) {
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

    Expression arg0 = args.get(0);
    if (op instanceof OptionalOperators) {
      // use IS_DEFINED / IS_UNDEFINED functions
      String path = arg0.accept(this).oql();
      String expr;
      if (op == OptionalOperators.IS_PRESENT) {
        expr = String.format("is_defined(%s) AND %s != null", path, path);
      } else {
        expr = String.format("is_undefined(%s) OR %s = null", path, path);
      }
      return oql(expr);
    } else if (op == Operators.NOT) {
      return oql("NOT (" + arg0.accept(this).oql() + ")");
    }  else if (op == IterableOperators.IS_EMPTY || op == StringOperators.TO_LOWER_CASE || op == StringOperators.TO_UPPER_CASE) {
      return oql(arg0.accept(this).oql() + "." + toMethodName(op));
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

    Expression left = args.get(0); // left node
    Expression right = args.get(1); // right node
    if (op == IterableOperators.CONTAINS || op == StringOperators.MATCHES
            || op == StringOperators.CONTAINS || op == StringOperators.STARTS_WITH
            || op == StringOperators.ENDS_WITH) {
      return oql(String.format("%s.%s(%s)", left.accept(this).oql(), toMethodName(op), right.accept(this).oql()));
    }

    if (op == StringOperators.HAS_LENGTH || op == IterableOperators.HAS_SIZE) {
      return oql(String.format("%s.%s = %s", left.accept(this).oql(), toMethodName(op), right.accept(this).oql()));
    }

    final String operator;
    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
      operator = op == Operators.EQUAL ? "=" : "!=";
    } else if (op == Operators.IN || op == Operators.NOT_IN) {
      if (right instanceof Constant) {
        // optimization for IN / NOT IN operators
        // make constant value(s) distinct using Set
        Set<Object> newValues = ImmutableSet.copyOf(Visitors.toConstant(right).values());
        right = Expressions.constant(newValues);
      }
      operator = op == Operators.IN ? "IN" : "NOT IN";
    } else if (op == ComparableOperators.GREATER_THAN) {
      operator = ">";
    } else if (op == ComparableOperators.GREATER_THAN_OR_EQUAL) {
      operator = ">=";
    } else if (op == ComparableOperators.LESS_THAN) {
      operator = "<";
    } else if (op == ComparableOperators.LESS_THAN_OR_EQUAL) {
      operator = "<=";
    } else {
      throw new IllegalArgumentException("Unknown binary operator " + call);
    }

    return oql(String.format("%s %s %s", left.accept(this).oql(), operator, right.accept(this).oql()));
  }

  @Override
  public Oql visit(Path path) {
    String name = pathNaming.name(path);
    Type type = path.returnType();
    if (!useBindVariables && (type instanceof Class<?>) && ((Class<?>) type).isEnum()) {
      // for enums we need to add ".name" function (queries without bind variables)
      // OQL should be "enum.name = 'VALUE'"
      name = name + ".name";
    }
    return oql(name);
  }

  @Override
  public Oql visit(Constant constant) {
    String oqlAsString;
    if (useBindVariables) {
      variables.add(constant.value());
      oqlAsString = "$" + variables.size();
    } else {
      oqlAsString = valueToString(constant.value());
    }

    return oql(oqlAsString);
  }

  /**
   * Return new query but with same variables
   */
  private Oql oql(String oql) {
    return new Oql(variables, oql);
  }

  private static String toMethodName(Operator op) {
    if (op == IterableOperators.IS_EMPTY) {
      return "isEmpty";
    } else if (op == StringOperators.TO_LOWER_CASE) {
      return "toLowerCase";
    } else if (op == StringOperators.TO_UPPER_CASE) {
      return "toUpperCase";
    } else if (op == StringOperators.HAS_LENGTH) {
      return "length";
    } else if (op == IterableOperators.HAS_SIZE) {
      return "size";
    } else if (op == StringOperators.CONTAINS || op == IterableOperators.CONTAINS) {
      return "contains";
    } else if (op == StringOperators.STARTS_WITH) {
      return "startsWith";
    } else if (op == StringOperators.ENDS_WITH) {
      return "endsWith";
    } else if (op == StringOperators.MATCHES) {
      return "matches";
    }

    throw new UnsupportedOperationException("Don't know how to handle Operator " + op);
  }

  private static String valueToString(Object value) {
    return OqlLiterals.fromObject(value);
  }

}
