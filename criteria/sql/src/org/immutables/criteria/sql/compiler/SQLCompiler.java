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

import org.immutables.criteria.expression.*;
import org.immutables.criteria.sql.SQLException;
import org.immutables.criteria.sql.SQLSetup;
import org.immutables.criteria.sql.reflection.SQLPropertyMetadata;

import java.util.*;
import java.util.stream.Collectors;

public class SQLCompiler {
    public static SQLCountStatement count(final SQLSetup setup, final Query query) {
        // TODO: Aggregations
        return ImmutableSQLCountStatement.builder()
                .table(setup.metadata().table())
                .columns(new TreeSet<>(setup.metadata().columns().keySet()))
                .distinct(query.distinct())
                .filter(compileWhereClause(setup, query.filter()))
                .ordering(compileOrderBy(setup, query.collations()))
                .qualifier(compileDistinctCount(setup, query).orElse("COUNT(*)"))
                .offset(query.offset())
                .limit(query.limit())
                .type(Long.class)
                .build();
    }

    public static SQLSelectStatement select(final SQLSetup setup, final Query query) {
        // TODO: Projections, Aggregations
        return ImmutableSQLSelectStatement.builder()
                .table(setup.metadata().table())
                .columns(new TreeSet<>(setup.metadata().columns().keySet()))
                .distinct(query.distinct())
                .filter(compileWhereClause(setup, query.filter()))
                .ordering(compileOrderBy(setup, query.collations()))
                .offset(query.offset())
                .limit(query.limit())
                .type(List.class)
                .build();
    }

    private static Optional<String> compileOrderBy(final SQLSetup setup, final List<Collation> collations) {
        final String ordering = collations.stream()
                .map(c -> String.format("`%s` %s",
                        setup.metadata().properties().get(c.path().toString()).mapping().name(),
                        c.direction().isAscending() ? "ASC" : "DESC"))
                .collect(Collectors.joining(","));
        return ordering.length() > 0 ? Optional.of(ordering) : Optional.empty();
    }

    private static Optional<String> compileDistinctCount(final SQLSetup setup, final Query query) {
        if (query.distinct()) {
            if (query.projections().size() != 1) {
                throw new SQLException("Expected a single projection argument to count with distinct");
            }
            return Optional.of(String.format("COUNT(DISTINCT `%s`)",
                    setup.metadata().properties().get(query.projections().get(0).toString()).mapping().name()));
        }
        return Optional.empty();
    }

    public static SQLDeleteStatement delete(final SQLSetup setup, final Query query) {
        return ImmutableSQLDeleteStatement.builder()
                .table(setup.metadata().table())
                .filter(compileWhereClause(setup, query.filter()))
                .type(Long.class)
                .build();
    }

    public static SQLInsertStatement insert(final SQLSetup setup, final List<Object> entities) {
        return ImmutableSQLInsertStatement.builder()
                .table(setup.metadata().table())
                .columns(new TreeSet<>(setup.metadata().columns().keySet()))
                .values(toPropertyMap(setup, entities))
                .type(Long.class)
                .build();
    }

    public static SQLUpdateStatement update(final SQLSetup setup, final Query query, final Map<Expression, Object> values) {
        final Map<String, SQLConstantExpression> updates = new HashMap<>();
        for (final Map.Entry<Expression, Object> e : values.entrySet()) {
            final Path path = (Path) e.getKey();
            final Object value = e.getValue();
            final SQLPropertyMetadata p = setup.metadata().properties().get(path.toString());
            updates.put(p.mapping().name(), ImmutableSQLConstantExpression.builder()
                    .sql(":" + p.mapping().name())
                    .type(p.type())
                    .value(value)
                    .target(p)
                    .build());
        }
        return ImmutableSQLUpdateStatement.builder()
                .table(setup.metadata().table())
                .filter(compileWhereClause(setup, query.filter()))
                .updates(updates)
                .type(Long.class)
                .build();
    }

    public static SQLSaveStatement save(final SQLSetup setup, final List<Object> entities) {
        final Class<?> type = setup.metadata().type();
        if (!(setup.metadata().key().metadata().isKeyDefined() && setup.metadata().key().metadata().isExpression())) {
            throw new SQLException("Update using objects requires a simple key to be defined");
        }

        final List<Map<String, SQLConstantExpression>> values = new ArrayList<>();
        for (final Object o : entities) {
            if (!type.isAssignableFrom(o.getClass())) {
                throw new SQLException(String.format("Incompatible save() type. Expected %s found %s",
                        type.getSimpleName(), o.getClass().getSimpleName()));
            }
        }
        return ImmutableSQLSaveStatement.builder()
                .table(setup.metadata().table())
                .key(setup.metadata().key().metadata().keys().get(0).toString())
                .columns(new TreeSet<>(setup.metadata().columns().keySet()))
                .properties(toPropertyMap(setup, entities))
                .type(Long.class)
                .build();
    }

    private static Optional<SQLFilterExpression> compileWhereClause(final SQLSetup setup, final Optional<Expression> filter) {
        if (filter.isPresent()) {
            if (!(filter.get() instanceof Call)) {
                throw new SQLException("Filter expression must be a call");
            }
            final SQLQueryVisitor visitor = new SQLQueryVisitor(setup);
            return Optional.of(visitor.call((Call) filter.get()));
        }
        return Optional.empty();

    }

    private static List<Map<String, SQLConstantExpression>> toPropertyMap(final SQLSetup setup, final List<Object> entities) {
        final Class<?> type = setup.metadata().type();
        final List<Map<String, SQLConstantExpression>> values = new ArrayList<>();
        for (final Object o : entities) {
            // Sanity check that all the objects in the list match the metadata type
            if (!type.isAssignableFrom(o.getClass())) {
                throw new SQLException(String.format("Incompatible insert() type. Expected %s found %s",
                        type.getSimpleName(), o.getClass().getSimpleName()));
            }
            final Map<String, SQLConstantExpression> row = new HashMap<>();
            for (final SQLPropertyMetadata p : setup.metadata().properties().values()) {
                final Object value = p.extractor().extract(o);
                row.put(p.mapping().name(), ImmutableSQLConstantExpression.builder()
                        .sql(p.mapping().name())
                        .type(p.type())
                        .value(value)
                        .target(p)
                        .build());
            }
            values.add(row);
        }
        return values;
    }
}
