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
import org.immutables.criteria.backend.StandardOperations.Update;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.sql.SqlBackend;
import org.immutables.criteria.sql.SqlSetup;
import org.immutables.criteria.sql.compiler.SqlCompiler;
import org.immutables.criteria.sql.compiler.SqlConstantExpression;
import org.immutables.criteria.sql.compiler.SqlSaveStatement;
import org.immutables.criteria.sql.conversion.TypeConverters;
import org.immutables.criteria.sql.jdbc.FluentStatement;
import org.reactivestreams.Publisher;

import java.util.*;
import java.util.concurrent.Callable;

public class SqlSaveCommand implements SqlCommand {

  private final SqlSetup setup;
  private final Update operation;
  private final SqlBackend.SQLSession session;

  public SqlSaveCommand(final SqlBackend.SQLSession session, final SqlSetup setup, final Update operation) {
    assert session != null : "session cannot be null";
    assert setup != null : "setup cannot be null";
    assert operation != null : "operation cannot be null";
    assert operation.values() != null : "update requires values";

    this.session = session;
    this.operation = operation;
    this.setup = setup;
  }

  private static List<List<Object>> values(final SqlSetup setup, final SqlSaveStatement statement) {
    final List<List<Object>> ret = new ArrayList<>();
    final Set<String> properties = new TreeSet<>(statement.columns());
    properties.remove(statement.key());
    for (final Map<String, SqlConstantExpression> entity : statement.properties()) {
      final List<Object> l = new ArrayList<>();
      for (final String property : properties) {
        final SqlConstantExpression c = entity.get(property);
        l.add(TypeConverters.convert(c.type(), c.target().mapping().type(), c.value()));
      }
      // Add key as the last parameters
      final SqlConstantExpression c = entity.get(statement.key());
      l.add(TypeConverters.convert(c.type(), c.target().mapping().type(), c.value()));
      ret.add(l);
    }
    return ret;
  }

  @Override
  public Publisher<?> execute() {
    final Callable<WriteResult> callable = toCallable(session, operation);
    return Flowable.fromCallable(callable);
  }

  private Callable<WriteResult> toCallable(final SqlBackend.SQLSession session, final Update operation) {
    return () -> {
      assert operation != null : "Missing `operation` parameter";
      assert operation.values() != null : "Expected `operation.values()` on update";

      // Short circuit empty update
      if (operation.values().size() == 0) {
        return WriteResult.empty().withInsertedCount(0);
      }
      final SqlSaveStatement save = SqlCompiler.save(setup, operation.values());
      try (final FluentStatement statement = FluentStatement.of(session.setup().datasource(),
          session.setup().dialect().save(save))) {
        final int result = statement.update(values(setup, save));
        return WriteResult.empty().withUpdatedCount(result);
      } catch (final Throwable t) {
        throw Throwables.propagate(t);
      }
    };
  }
}
