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
import org.immutables.criteria.sql.SQLBackend;
import org.immutables.criteria.sql.SQLSetup;
import org.immutables.criteria.sql.compiler.SQLCompiler;
import org.immutables.criteria.sql.compiler.SQLFilterExpression;
import org.immutables.criteria.sql.compiler.SQLSelectStatement;
import org.immutables.criteria.sql.conversion.RowMappers;
import org.immutables.criteria.sql.jdbc.FluentStatement;
import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.concurrent.Callable;

public class SQLSelectCommand implements SQLCommand {

    private final SQLSetup setup;
    private final Select operation;
    private final SQLBackend.SQLSession session;

    public SQLSelectCommand(final SQLBackend.SQLSession session, final SQLSetup setup, final Select operation) {
        assert session != null : "session cannot be null";
        assert setup != null : "setup cannot be null";
        assert operation != null : "operation cannot be null";
        assert operation.query().count() == false : "count() query unexpected";

        this.session = session;
        this.operation = operation;
        this.setup = setup;
    }

    @Override
    public Publisher<?> execute() {
        final Callable<Iterable<?>> callable = toCallable(session, operation);
        return Flowable.fromCallable(callable).flatMapIterable(x -> x);
    }

    private Callable<Iterable<?>> toCallable(final SQLBackend.SQLSession session, final Select operation) {
        return () -> {
            assert operation != null : "Missing `operation` parameter";
            assert operation.query() != null : "Missing `operation.query()` parameter";

            final SQLSelectStatement select = SQLCompiler.select(setup, operation.query());
            try (final FluentStatement statement = FluentStatement.of(session.setup().datasource(),
                    session.setup().dialect().select(select))) {
                return statement
                        .set(toParameters(select
                                .filter()
                                .map(SQLFilterExpression::parameters)
                                .orElse(Collections.emptyMap())))
                        .list((rs) -> RowMappers.get(session.metadata()).map(rs));
            } catch (final Throwable t) {
                throw Throwables.propagate(t);
            }
        };
    }
}
