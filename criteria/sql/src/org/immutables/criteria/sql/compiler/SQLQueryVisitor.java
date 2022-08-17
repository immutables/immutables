package org.immutables.criteria.sql.compiler;

import com.fasterxml.jackson.databind.type.TypeFactory;
import org.immutables.criteria.expression.*;
import org.immutables.criteria.sql.SQLException;
import org.immutables.criteria.sql.SQLSetup;
import org.immutables.criteria.sql.reflection.SQLPropertyMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SQLQueryVisitor {
    private final Map<String, SQLConstantExpression> parameters = new HashMap<>();
    private final SQLSetup setup;

    public SQLQueryVisitor(final SQLSetup setup) {
        this.setup = setup;
    }

    private static SQLFilterExpression unary(final Call call) {
        return null;
    }

    private static SQLFilterExpression ternary(final Call call) {
        return null;
    }

    public SQLFilterExpression call(final Call call) {
        if (call.operator().arity() == Operator.Arity.UNARY) {
            return unary(call);
        }
        if (call.operator().arity() == Operator.Arity.BINARY) {
            return binary(call);
        }
        if (call.operator().arity() == Operator.Arity.TERNARY) {
            return ternary(call);
        }
        throw new SQLException("Invalid operator arity " + call.operator());
    }

    public SQLConstantExpression constant(final SQLNameExpression target, final Constant constant) {
        final String params = constant.values().stream().map(e -> {
            final String name = String.format(":param_%d", parameters.size());
            final SQLConstantExpression result = ImmutableSQLConstantExpression.builder()
                    .sql(name)
                    .target(target.metadata())
                    .value(e)
                    .type(TypeFactory.rawClass(constant.returnType()))
                    .build();
            parameters.put(name, result);
            return name;
        }).collect(Collectors.joining(","));
        final SQLConstantExpression result = ImmutableSQLConstantExpression.builder()
                .sql(params)
                .target(target.metadata())
                .value(constant.value())
                .type(TypeFactory.rawClass(constant.returnType()))
                .build();
        return result;
    }

    public SQLNameExpression path(final Path path) {
        final SQLPropertyMetadata meta = setup.metadata()
                .properties()
                .get(path.toString());
        return ImmutableSQLNameExpression.builder()
                // TODO: Column aliases
                .sql("`" + meta.mapping().name() + "`")
                .metadata(meta)
                .type(meta.mapping().type())
                .build();
    }

    private SQLFilterExpression logical(final Call call) {
        final Operator op = call.operator();
        final List<Expression> args = call.arguments();

        if (!(args.get(0) instanceof Call)) {
            throw new SQLException("left hand side of logical expression must be a call");
        }
        if (!(args.get(1) instanceof Call)) {
            throw new SQLException("right hand side of logical expression must be a call");
        }
        final String sql = setup.dialect().logical((Operators) op,
                call((Call) args.get(0)),
                call((Call) args.get(1)));
        return ImmutableSQLFilterExpression.builder()
                .sql(sql)
                .parameters(parameters)
                .type(TypeFactory.rawClass(args.get(0).returnType()))
                .build();

    }

    private SQLFilterExpression binary(final Call call) {
        final Operator op = call.operator();
        final List<Expression> args = call.arguments();
        if (args.size() != 2) {
            throw new SQLException("binary expression expects exactly 2 args");
        }
        if (op == Operators.AND || op == Operators.OR) {
            return logical(call);
        }
        if (!(args.get(0) instanceof Path)) {
            throw new SQLException("left hand side of comparison should be a path");
        }
        if (!(args.get(1) instanceof Constant)) {
            throw new SQLException("right hand side of comparison should be a constant");
        }
        final Path left = (Path) args.get(0);
        final Constant right = (Constant) args.get(1);
        final SQLNameExpression target = path(left);
        final Optional<String> operator = setup.dialect().binary(op, target, constant(target, right));
        return operator.map(s -> ImmutableSQLFilterExpression.builder()
                .sql(s)
                .parameters(parameters)
                .type(TypeFactory.rawClass(left.returnType()))
                .build()).orElseThrow(() ->
                new SQLException("Unhandled binary operator " + op));
    }
}
