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
package org.immutables.criteria.sql.dialects;

import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.ComparableOperators;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.StringOperators;
import org.immutables.criteria.sql.SqlException;
import org.immutables.criteria.sql.compiler.*;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SQL92Dialect implements SqlDialect {
  private final PathNaming naming = new SqlPathNaming();

  @Override
  public Optional<String> limit(final OptionalLong limit, final OptionalLong offset) {
    if (limit.isPresent() || offset.isPresent()) {
      final String l = limit.isPresent() ? String.format("LIMIT %d", limit.getAsLong()) : "";
      final String o = offset.isPresent() ? String.format("OFFSET %d", offset.getAsLong()) : "";
      return Optional.of(String.format("%s %s", l, o).trim());
    }
    return Optional.empty();
  }

  @Override
  public String logical(final Operators op, final SqlExpression left, final SqlExpression right) {
    switch (op) {
      case AND:
        return String.format("(%s)AND(%s)", left.sql(), right.sql());
      case OR:
        return String.format("(%s)OR(%s)", left.sql(), right.sql());
    }
    throw new SqlException("Invalid logical operator " + op.name());
  }

  @Override
  public Optional<String> equality(final Operators op, final SqlExpression left, final SqlExpression right) {
    if (!(left instanceof SqlNameExpression)) {
      throw new SqlException("left hand side of comparison should be a path/name");
    }
    if (!(right instanceof SqlConstantExpression)) {
      throw new SqlException("right hand side of comparison should be a constant");
    }
    switch (op) {
      case EQUAL:
        return Optional.of(String.format("(%s)=(%s)", left.sql(), right.sql()));
      case NOT_EQUAL:
        return Optional.of(String.format("(%s)!=(%s)", left.sql(), right.sql()));
      case IN:
        return Optional.of(String.format("(%s)IN(%s)", left.sql(), right.sql()));
      case NOT_IN:
        return Optional.of(String.format("(%s)NOT IN(%s)", left.sql(), right.sql()));
    }
    throw new SqlException("Invalid logical operator " + op.name());
  }

  @Override
  public Optional<String> comparison(final ComparableOperators op, final SqlExpression left, final SqlExpression right) {
    switch (op) {
      case LESS_THAN:
        return Optional.of(String.format("(%s)<(%s)", left.sql(), right.sql()));
      case LESS_THAN_OR_EQUAL:
        return Optional.of(String.format("(%s)<=(%s)", left.sql(), right.sql()));
      case GREATER_THAN:
        return Optional.of(String.format("(%s)>(%s)", left.sql(), right.sql()));
      case GREATER_THAN_OR_EQUAL:
        return Optional.of(String.format("(%s)>=(%s)", left.sql(), right.sql()));
    }
    throw new SqlException("Invalid comparison operator " + op.name());
  }

  @Override
  public Optional<String> regex(final Operator op, final SqlExpression left, final Pattern right) {
    // TODO: Verify that the regex does not contain other patterns
    final String converted = right.pattern()
        .replace(".*", "%")
        .replace(".?", "%")
        .replace("'", "''")
        .replace('?', '%')
        .replace('.', '_');
    if ((StringOperators) op == StringOperators.MATCHES) {
      return Optional.of(String.format("(%s)LIKE('%s')", left.sql(), converted));
    }
    throw new SqlException("Invalid regex operator " + op.name());
  }

  @Override
  public Optional<String> string(final StringOperators op, final SqlExpression left, final SqlExpression right) {
    switch (op) {
      case HAS_LENGTH:
        return Optional.of(String.format("LEN(%s)=(%s)", left.sql(), right.sql()));
      case CONTAINS:
        return Optional.of(String.format("(%s)LIKE(CONCAT(\"%%\",%s,\"%%\"))", left.sql(), right.sql()));
      case ENDS_WITH:
        return Optional.of(String.format("(%s)LIKE(CONCAT(\"%%\",%s))", left.sql(), right.sql()));
      case STARTS_WITH:
        return Optional.of(String.format("(%s)LIKE(CONCAT(%s,\"%%\"))", left.sql(), right.sql()));
      case MATCHES:
        final Object arg = ((SqlConstantExpression) right).value();
        assert arg instanceof Pattern : "Argument to regex() should be Pattern";
        return regex(op, left, (Pattern) arg);
    }
    throw new SqlException("Invalid string operator " + op.name());
  }

  @Override
  public Optional<String> binary(final Operator op, final SqlExpression left, final SqlExpression right) {
    if (!(left instanceof SqlNameExpression)) {
      throw new SqlException("left hand side of comparison should be a path/name");
    }
    if (!(right instanceof SqlConstantExpression)) {
      throw new SqlException("right hand side of comparison should be a constant");
    }
    final SqlConstantExpression updated = ImmutableSqlConstantExpression
        .builder()
        .from((SqlConstantExpression) right)
        .target(((SqlNameExpression) left).metadata())
        .build();
    if (op instanceof Operators) {
      return equality((Operators) op, left, updated);
    }
    if (op instanceof StringOperators) {
      return string((StringOperators) op, left, updated);
    }
    if (op instanceof ComparableOperators) {
      return comparison((ComparableOperators) op, left, updated);
    }
    throw new SqlException("Unhandled operator: " + op.name());
  }

  @Override
  public PathNaming naming() {
    return naming;
  }

  @Override
  public String count(final SqlCountStatement statement) {
    return String.format("SELECT %s FROM `%s`%s%s %s",
            statement.qualifier(),
            statement.table(),
            statement.filter().map(s -> " WHERE " + s.sql()).orElse(""),
            statement.ordering().map(s -> " ORDER BY " + s).orElse(""),
            limit(statement.limit(), statement.offset()).orElse(""))
        .trim();
  }

  @Override
  public String select(final SqlSelectStatement statement) {
    return String.format("SELECT %s%s FROM `%s`%s%s %s",
            statement.distinct().map(e -> e ? "DISTINCT " : "").orElse(""),
            statement.columns().stream().map(c -> String.format("`%s`", c)).collect(Collectors.joining(",")),
            statement.table(),
            statement.filter().map(s -> " WHERE " + s.sql()).orElse(""),
            statement.ordering().map(s -> " ORDER BY " + s).orElse(""),
            limit(statement.limit(), statement.offset()).orElse(""))
        .trim();
  }

  @Override
  public String delete(final SqlDeleteStatement statement) {
    // TODO: Sorting
    return String.format("DELETE FROM `%s`%s",
            statement.table(),
            statement.filter().map(s -> " WHERE " + s.sql()).orElse(""))
        .trim();
  }

  @Override
  public String insert(final SqlInsertStatement statement) {
    return String.format("INSERT INTO %s (%s)\nVALUES\n%s",
            statement.table(),
            statement.columns().stream().map(c -> String.format("`%s`", c)).collect(Collectors.joining(",")),
            "(" + statement.columns().stream().map(c -> "?").collect(Collectors.joining(",")) + ")")
        .trim();
  }

  @Override
  public String update(final SqlUpdateStatement statement) {
    return String.format("UPDATE `%s` SET %s%s",
            statement.table(),
            statement.updates()
                .entrySet()
                .stream().map(e -> String.format("`%s`=:%s", e.getKey(), e.getKey()))
                .collect(Collectors.joining(",")),
            statement.filter().map(s -> " WHERE " + s.sql()).orElse(""))
        .trim();
  }

  @Override
  public String save(final SqlSaveStatement statement) {
    final Set<String> properties = new TreeSet<>(statement.columns());
    properties.remove(statement.key());
    return String.format("UPDATE `%s` SET %s WHERE %s=:%s",
            statement.table(),
            properties.stream()
                .map(e -> String.format("`%s`=:%s", e, e))
                .collect(Collectors.joining(",")),
            statement.key(),
            statement.key()
        )
        .trim();
  }
}
