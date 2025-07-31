/*
 * Copyright 2022 Immutables Authors and Contributors
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
package org.immutables.criteria.sql.compiler;

import com.google.common.reflect.TypeToken;
import org.immutables.criteria.expression.*;
import org.immutables.criteria.sql.SqlException;
import org.immutables.criteria.sql.SqlSetup;
import org.immutables.criteria.sql.reflection.SqlPropertyMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SqlQueryVisitor {
  private final Map<String, SqlConstantExpression> parameters = new HashMap<>();
  private final SqlSetup setup;

  public SqlQueryVisitor(final SqlSetup setup) {
    this.setup = setup;
  }

  private static SqlFilterExpression unary(final Call call) {
    return null;
  }

  private static SqlFilterExpression ternary(final Call call) {
    return null;
  }

  public SqlFilterExpression call(final Call call) {
    if (call.operator().arity() == Operator.Arity.UNARY) {
      return unary(call);
    }
    if (call.operator().arity() == Operator.Arity.BINARY) {
      return binary(call);
    }
    if (call.operator().arity() == Operator.Arity.TERNARY) {
      return ternary(call);
    }
    throw new SqlException("Invalid operator arity " + call.operator());
  }

  public SqlConstantExpression constant(final SqlNameExpression target, final Constant constant) {
    final String params = constant.values().stream().map(e -> {
      final String name = String.format(":param_%d", parameters.size());
      final SqlConstantExpression result = ImmutableSqlConstantExpression.builder()
          .sql(name)
          .target(target.metadata())
          .value(e)
          .type(TypeToken.of(constant.returnType()).getRawType())
          .build();
      parameters.put(name, result);
      return name;
    }).collect(Collectors.joining(","));
    final SqlConstantExpression result = ImmutableSqlConstantExpression.builder()
        .sql(params)
        .target(target.metadata())
        .value(constant.value())
        .type(TypeToken.of(constant.returnType()).getRawType())
        .build();
    return result;
  }

  public SqlNameExpression path(final Path path) {
    final SqlPropertyMetadata meta = setup.metadata()
        .properties()
        .get(path.toString());
    return ImmutableSqlNameExpression.builder()
        // TODO: Column aliases
        .sql("`" + meta.mapping().name() + "`")
        .metadata(meta)
        .type(meta.mapping().type())
        .build();
  }

  private SqlFilterExpression logical(final Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    if (!(args.get(0) instanceof Call)) {
      throw new SqlException("left hand side of logical expression must be a call");
    }
    if (!(args.get(1) instanceof Call)) {
      throw new SqlException("right hand side of logical expression must be a call");
    }
    final String sql = setup.dialect().logical((Operators) op,
        call((Call) args.get(0)),
        call((Call) args.get(1)));
    return ImmutableSqlFilterExpression.builder()
        .sql(sql)
        .parameters(parameters)
        .type(TypeToken.of(args.get(0).returnType()).getRawType())
        .build();

  }

  private SqlFilterExpression binary(final Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();
    if (args.size() != 2) {
      throw new SqlException("binary expression expects exactly 2 args");
    }
    if (op == Operators.AND || op == Operators.OR) {
      return logical(call);
    }
    if (!(args.get(0) instanceof Path)) {
      throw new SqlException("left hand side of comparison should be a path");
    }
    if (!(args.get(1) instanceof Constant)) {
      throw new SqlException("right hand side of comparison should be a constant");
    }
    final Path left = (Path) args.get(0);
    final Constant right = (Constant) args.get(1);
    final SqlNameExpression target = path(left);
    final Optional<String> operator = setup.dialect().binary(op, target, constant(target, right));
    return operator.map(s -> ImmutableSqlFilterExpression.builder()
        .sql(s)
        .parameters(parameters)
        .type(TypeToken.of(left.returnType()).getRawType())
        .build()).orElseThrow(() ->
        new SqlException("Unhandled binary operator " + op));
  }
}
