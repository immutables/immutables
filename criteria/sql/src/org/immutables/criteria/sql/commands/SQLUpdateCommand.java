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
import org.immutables.criteria.backend.StandardOperations.UpdateByQuery;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.sql.SQLBackend;
import org.immutables.criteria.sql.SQLSetup;
import org.immutables.criteria.sql.compiler.SQLCompiler;
import org.immutables.criteria.sql.compiler.SQLFilterExpression;
import org.immutables.criteria.sql.compiler.SQLUpdateStatement;
import org.immutables.criteria.sql.jdbc.FluentStatement;
import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLUpdateCommand implements SQLCommand {

    private final SQLSetup setup;
    private final UpdateByQuery operation;
    private final SQLBackend.SQLSession session;

    public SQLUpdateCommand(final SQLBackend.SQLSession session, final SQLSetup setup, final UpdateByQuery operation) {
        assert session != null : "session cannot be null";
        assert setup != null : "setup cannot be null";
        assert operation != null : "operation cannot be null";
        assert operation.query() != null : "update requires a query";

        this.session = session;
        this.operation = operation;
        this.setup = setup;
    }

    @Override
    public Publisher<?> execute() {
        final Callable<WriteResult> callable = toCallable(session, operation);
        return Flowable.fromCallable(callable);
    }

    private Callable<WriteResult> toCallable(final SQLBackend.SQLSession session, final UpdateByQuery operation) {
        return () -> {
            assert operation != null : "Missing `operation` parameter";
            assert operation.query() != null : "Missing `operation.query()` parameter";
            assert operation.values() != null : "Expected `operation.values()` on update";

            final SQLUpdateStatement update = SQLCompiler.update(setup, operation.query(), operation.values());
            try (final FluentStatement statement = FluentStatement.of(session.setup().datasource(),
                    session.setup().dialect().update(update))) {
                final Map<String, Object> merged = toParameters(Stream.concat(
                                update.updates().entrySet().stream(),
                                update.filter()
                                        .map(SQLFilterExpression::parameters)
                                        .orElse(Collections.emptyMap()).entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
                final int result = statement
                        .set(merged)
                        .update();
                return WriteResult.empty().withUpdatedCount(result);
            } catch (final Throwable t) {
                throw Throwables.propagate(t);
            }
        };
    }
}
