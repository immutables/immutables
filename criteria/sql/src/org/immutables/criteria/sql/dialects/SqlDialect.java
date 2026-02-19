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
import org.immutables.criteria.sql.compiler.*;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Pattern;

public interface SqlDialect {
  PathNaming naming();

  Optional<String> limit(final OptionalLong limit, final OptionalLong offset);

  Optional<String> string(StringOperators op, SqlExpression left, SqlExpression right);

  Optional<String> comparison(ComparableOperators op, SqlExpression left, SqlExpression right);

  String logical(Operators op, SqlExpression left, SqlExpression right);

  Optional<String> equality(Operators op, SqlExpression left, SqlExpression right);

  Optional<String> binary(Operator op, SqlExpression left, SqlExpression right);

  Optional<String> regex(Operator op, SqlExpression left, Pattern right);

  String count(SqlCountStatement statement);

  String select(SqlSelectStatement statement);

  String delete(SqlDeleteStatement statement);

  String insert(SqlInsertStatement statement);

  String update(SqlUpdateStatement statement);

  String save(SqlSaveStatement statement);
}
