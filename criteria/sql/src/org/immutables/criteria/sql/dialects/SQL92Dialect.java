package org.immutables.criteria.sql.dialects;

import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.ComparableOperators;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.StringOperators;
import org.immutables.criteria.sql.SQLException;
import org.immutables.criteria.sql.compiler.*;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SQL92Dialect implements SQLDialect {
    private final PathNaming naming = new SQLPathNaming();

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
    public String logical(final Operators op, final SQLExpression left, final SQLExpression right) {
        switch ((Operators) op) {
            case AND:
                return String.format("(%s)AND(%s)", left.sql(), right.sql());
            case OR:
                return String.format("(%s)OR(%s)", left.sql(), right.sql());
        }
        throw new SQLException("Invalid logical operator " + op.name());
    }

    @Override
    public Optional<String> equality(final Operators op, final SQLExpression left, final SQLExpression right) {
        if (!(left instanceof SQLNameExpression)) {
            throw new SQLException("left hand side of comparison should be a path/name");
        }
        if (!(right instanceof SQLConstantExpression)) {
            throw new SQLException("right hand side of comparison should be a constant");
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
        throw new SQLException("Invalid logical operator " + op.name());
    }

    @Override
    public Optional<String> comparison(final ComparableOperators op, final SQLExpression left, final SQLExpression right) {
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
        throw new SQLException("Invalid comparison operator " + op.name());
    }

    @Override
    public Optional<String> regex(final Operator op, final SQLExpression left, final Pattern right) {
        // TODO: Verify that the regex does not contain other patterns
        final String converted = right.pattern()
                .replace(".*", "%")
                .replace(".?", "%")
                .replace("'", "''")
                .replace('?', '%')
                .replace('.', '_');
        switch ((StringOperators) op) {
            case MATCHES:
                return Optional.of(String.format("(%s)LIKE('%s')", left.sql(), converted));
        }
        throw new SQLException("Invalid regex operator " + op.name());
    }

    @Override
    public Optional<String> string(final StringOperators op, final SQLExpression left, final SQLExpression right) {
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
                final Object arg = ((SQLConstantExpression) right).value();
                assert arg instanceof Pattern : "Argument to regex() should be Pattern";
                return regex(op, left, (Pattern) arg);
        }
        throw new SQLException("Invalid string operator " + op.name());
    }

    @Override
    public Optional<String> binary(final Operator op, final SQLExpression left, final SQLExpression right) {
        if (!(left instanceof SQLNameExpression)) {
            throw new SQLException("left hand side of comparison should be a path/name");
        }
        if (!(right instanceof SQLConstantExpression)) {
            throw new SQLException("right hand side of comparison should be a constant");
        }
        final SQLConstantExpression updated = ImmutableSQLConstantExpression
                .builder()
                .from((SQLConstantExpression) right)
                .target(((SQLNameExpression) left).metadata())
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
        throw new SQLException("Unhandled operator: " + op.name());
    }

    @Override
    public PathNaming naming() {
        return naming;
    }

    @Override
    public String count(final SQLCountStatement statement) {
        return String.format("SELECT %s FROM `%s`%s%s %s",
                        statement.qualifier(),
                        statement.table(),
                        statement.filter().map(s -> " WHERE " + s.sql()).orElse(""),
                        statement.ordering().map(s -> " ORDER BY " + s).orElse(""),
                        limit(statement.limit(), statement.offset()).orElse(""))
                .trim();
    }

    @Override
    public String select(final SQLSelectStatement statement) {
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
    public String delete(final SQLDeleteStatement statement) {
        // TODO: Sorting
        return String.format("DELETE FROM `%s`%s",
                        statement.table(),
                        statement.filter().map(s -> " WHERE " + s.sql()).orElse(""))
                .trim();
    }

    @Override
    public String insert(final SQLInsertStatement statement) {
        return String.format("INSERT INTO %s (%s)\nVALUES\n%s",
                        statement.table(),
                        statement.columns().stream().map(c -> String.format("`%s`", c)).collect(Collectors.joining(",")),
                        "(" + statement.columns().stream().map(c -> "?").collect(Collectors.joining(",")) + ")")
                .trim();
    }

    @Override
    public String update(final SQLUpdateStatement statement) {
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
    public String save(final SQLSaveStatement statement) {
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
