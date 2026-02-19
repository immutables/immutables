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
package org.immutables.criteria.sql.commands;

import com.google.common.base.Throwables;
import io.reactivex.Flowable;
import org.immutables.criteria.backend.StandardOperations.Select;
import org.immutables.criteria.sql.SqlBackend;
import org.immutables.criteria.sql.SqlException;
import org.immutables.criteria.sql.SqlSetup;
import org.immutables.criteria.sql.compiler.SqlCompiler;
import org.immutables.criteria.sql.compiler.SqlCountStatement;
import org.immutables.criteria.sql.compiler.SqlFilterExpression;
import org.immutables.criteria.sql.conversion.RowMappers;
import org.immutables.criteria.sql.jdbc.FluentStatement;
import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;

public class SqlCountCommand implements SqlCommand {

  private final SqlSetup setup;
  private final Select operation;
  private final SqlBackend.SQLSession session;

  public SqlCountCommand(final SqlBackend.SQLSession session, final SqlSetup setup, final Select operation) {
    assert session != null : "session cannot be null";
    assert setup != null : "setup cannot be null";
    assert operation != null : "operation cannot be null";
    assert operation.query().count() : "count() query expected";

    this.session = session;
    this.operation = operation;
    this.setup = setup;
  }

  @Override
  public Publisher<Long> execute() {
    final Callable<Long> callable = toCallable(session, operation);
    return Flowable.fromCallable(callable);
  }

  private Callable<Long> toCallable(final SqlBackend.SQLSession session, final Select operation) {
    return () -> {
      assert operation != null : "Missing `operation` parameter";
      assert operation.query() != null : "Missing `operation.query()` parameter";

      final SqlCountStatement count = SqlCompiler.count(setup, operation.query());
      try (final FluentStatement statement = FluentStatement.of(session.setup().datasource(),
          session.setup().dialect().count(count))) {
        final Optional<Long> ret = statement
            .set(toParameters(count
                .filter()
                .map(SqlFilterExpression::parameters)
                .orElse(Collections.emptyMap())))
            .list((rs) -> RowMappers.get(Long.class).map(rs))
            .stream()
            .findFirst();
        return ret.orElseThrow(() -> new SqlException("No results returned from count()"));
      } catch (final Throwable t) {
        throw Throwables.propagate(t);
      }
    };
  }
}
