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
import org.immutables.criteria.backend.StandardOperations.Insert;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.sql.SQLBackend;
import org.immutables.criteria.sql.SQLSetup;
import org.immutables.criteria.sql.compiler.SQLCompiler;
import org.immutables.criteria.sql.compiler.SQLConstantExpression;
import org.immutables.criteria.sql.compiler.SQLInsertStatement;
import org.immutables.criteria.sql.conversion.TypeConverters;
import org.immutables.criteria.sql.jdbc.FluentStatement;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class SQLInsertCommand implements SQLCommand {

    private final SQLSetup setup;
    private final Insert operation;
    private final SQLBackend.SQLSession session;

    public SQLInsertCommand(final SQLBackend.SQLSession session, final SQLSetup setup, final Insert operation) {
        assert session != null : "session cannot be null";
        assert setup != null : "setup cannot be null";
        assert operation != null : "operation cannot be null";
        assert operation.values().size() > 0 : "insert requires at least 1 object";

        this.session = session;
        this.operation = operation;
        this.setup = setup;
    }

    private static List<List<Object>> values(final SQLSetup setup, final SQLInsertStatement statement) {
        final List<List<Object>> ret = new ArrayList<>();
        for (final Map<String, SQLConstantExpression> row : statement.values()) {
            final List<Object> l = new ArrayList<>();
            for (final String column : statement.columns()) {
                final SQLConstantExpression c = row.get(column);
                l.add(TypeConverters.convert(c.type(), c.target().mapping().type(), c.value()));
            }
            ret.add(l);
        }
        return ret;
    }

    @Override
    public Publisher<?> execute() {
        final Callable<WriteResult> callable = toCallable(session, operation);
        return Flowable.fromCallable(callable);
    }

    private Callable<WriteResult> toCallable(final SQLBackend.SQLSession session, final Insert operation) {
        return () -> {
            assert operation != null : "Missing `operation` parameter";
            assert operation.values() != null : "Missing `operation.values()` parameter";

            // Short circuit empty insert
            if (operation.values().size() == 0) {
                return WriteResult.empty().withInsertedCount(0);
            }

            final SQLInsertStatement insert = SQLCompiler.insert(setup, operation.values());
            try (final FluentStatement statement = FluentStatement.of(session.setup().datasource(),
                    session.setup().dialect().insert(insert))) {
                final int result = statement.insert(values(setup, insert));
                return WriteResult.empty().withInsertedCount(result);
            } catch (final Throwable t) {
                throw Throwables.propagate(t);
            }
        };
    }
}
