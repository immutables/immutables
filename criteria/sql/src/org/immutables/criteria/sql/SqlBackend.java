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
package org.immutables.criteria.sql;

import io.reactivex.Flowable;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.DefaultResult;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.sql.commands.*;
import org.immutables.criteria.sql.reflection.SqlTypeMetadata;
import org.reactivestreams.Publisher;

/**
 * Implementation of {@code Backend} which delegates to the underlying SQL specific commands.
 */
public class SqlBackend implements Backend {
  private final SqlSetup setup;

  public SqlBackend(final SqlSetup setup) {
    this.setup = setup;
  }

  public static SqlBackend of(final SqlSetup setup) {
    return new SqlBackend(setup);
  }

  @Override
  public Session open(final Class<?> type) {
    return new SQLSession(type, setup);
  }

  public static class SQLSession implements Backend.Session {
    private final Class<?> type;
    private final SqlSetup setup;

    private final SqlTypeMetadata metadata;

    SQLSession(final Class<?> type, final SqlSetup setup) {
      this.type = type;
      this.setup = setup;

      metadata = SqlTypeMetadata.of(type);
    }

    public SqlSetup setup() {
      return setup;
    }

    public SqlTypeMetadata metadata() {
      return metadata;
    }

    @Override
    public Class<?> entityType() {
      return type;
    }

    @Override
    public Result execute(final Operation operation) {
      return DefaultResult.of(Flowable.defer(() -> executeInternal(operation)));
    }

    private Publisher<?> executeInternal(final Operation operation) {
      if (operation instanceof StandardOperations.Select) {
        final StandardOperations.Select select = (StandardOperations.Select) operation;
        final SqlCommand command = select.query().count()
            ? new SqlCountCommand(this, setup, select)
            : new SqlSelectCommand(this, setup, select);
        return command.execute();
      } else if (operation instanceof StandardOperations.Update) {
        final StandardOperations.Update update = (StandardOperations.Update) operation;
        final SqlCommand command = new SqlSaveCommand(this, setup, update);
        return command.execute();
      } else if (operation instanceof StandardOperations.UpdateByQuery) {
        final StandardOperations.UpdateByQuery update = (StandardOperations.UpdateByQuery) operation;
        final SqlCommand command = new SqlUpdateCommand(this, setup, update);
        return command.execute();
      } else if (operation instanceof StandardOperations.Insert) {
        final StandardOperations.Insert insert = (StandardOperations.Insert) operation;
        final SqlCommand command = new SqlInsertCommand(this, setup, insert);
        return command.execute();
      } else if (operation instanceof StandardOperations.Delete) {
        final StandardOperations.Delete delete = (StandardOperations.Delete) operation;
        final SqlCommand command = new SqlDeleteCommand(this, setup, delete);
        return command.execute();
      } else if (operation instanceof StandardOperations.Watch) {
        throw new UnsupportedOperationException("Watch");
      } else if (operation instanceof StandardOperations.DeleteByKey) {
        throw new UnsupportedOperationException("DeleteByKey");
      } else if (operation instanceof StandardOperations.GetByKey) {
        throw new UnsupportedOperationException("GetByKey");
      }

      return Flowable.error(new UnsupportedOperationException(String.format("Operation %s not supported by %s",
          operation, SqlBackend.class.getSimpleName())));
    }
  }
}
