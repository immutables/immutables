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

public interface SQLDialect {
    PathNaming naming();

    Optional<String> limit(final OptionalLong limit, final OptionalLong offset);

    Optional<String> string(StringOperators op, SQLExpression left, SQLExpression right);

    Optional<String> comparison(ComparableOperators op, SQLExpression left, SQLExpression right);

    String logical(Operators op, SQLExpression left, SQLExpression right);

    Optional<String> equality(Operators op, SQLExpression left, SQLExpression right);

    Optional<String> binary(Operator op, SQLExpression left, SQLExpression right);

    Optional<String> regex(Operator op, SQLExpression left, Pattern right);

    String count(SQLCountStatement statement);

    String select(SQLSelectStatement statement);

    String delete(SQLDeleteStatement statement);

    String insert(SQLInsertStatement statement);

    String update(SQLUpdateStatement statement);

    String save(SQLSaveStatement statement);
}
