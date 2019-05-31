package org.immutables.criteria.elasticsearch;

import com.google.common.base.Preconditions;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Constant;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.ExpressionVisitor;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.OperatorTables;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Path;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Evaluating expression into <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html">Elastic Query DSL</a>
 */
public class ElasticsearchQueryVisitor implements ExpressionVisitor<QueryBuilders.QueryBuilder> {


  @Override
  public QueryBuilders.QueryBuilder visit(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = args.get(0).accept(new PathVisitor());
      final Object right = args.get(1).accept(new ConstantVisitor());

      QueryBuilders.QueryBuilder builder = QueryBuilders.termQuery(field, right);
      if (op == Operators.NOT_EQUAL) {
        builder = QueryBuilders.boolQuery().mustNot(builder);
      }

      return builder;
    }

    if (op == Operators.IN || op == Operators.NOT_IN) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = args.get(0).accept(new PathVisitor());
      @SuppressWarnings("unchecked")
      final Iterable<Object> values = (Iterable<Object>) args.get(1).accept(new ConstantVisitor());
      Preconditions.checkNotNull(values, "not expected to be null %s", args.get(1));

      QueryBuilders.QueryBuilder builder = QueryBuilders.termsQuery(field, values);

      if (op == Operators.NOT_IN) {
        builder = QueryBuilders.boolQuery().mustNot(builder);
      }

      return builder;
    }

    if (op == Operators.AND || op == Operators.OR) {
      Preconditions.checkArgument(!args.isEmpty(), "Size should be >=1 for %s but was %s", op, args.size());

      final QueryBuilders.BoolQueryBuilder builder = args.stream()
              .map(a -> a.accept(this))
              .reduce(QueryBuilders.boolQuery(), (a, b) -> op == Operators.AND ? a.must(b) : a.should(b), (a, b) -> b);

      return builder;
    }

    if (op == Operators.NOT) {
      Preconditions.checkArgument(args.size() == 1, "Size should be 1 for %s but was %s", op, args.size());
      final QueryBuilders.QueryBuilder builder = args.get(0).accept(this);
      return QueryBuilders.boolQuery().mustNot(builder);
    }

    if (OperatorTables.COMPARISON.contains(op)) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = args.get(0).accept(new PathVisitor());
      final Object value = args.get(1).accept(new ConstantVisitor());
      final QueryBuilders.RangeQueryBuilder builder = QueryBuilders.rangeQuery(field);

      if (op == Operators.GREATER_THAN) {
        builder.gt(value);
      } else if (op == Operators.GREATER_THAN_OR_EQUAL) {
        builder.gte(value);
      } else if (op == Operators.LESS_THAN) {
        builder.lt(value);
      } else if (op == Operators.LESS_THAN_OR_EQUAL) {
        builder.lte(value);
      } else {
        throw new UnsupportedOperationException("Unknown comparison " + call);
      }

      return builder;
    }


    throw new UnsupportedOperationException("Don't know how to handle " + call);
  }

  @Override
  public QueryBuilders.QueryBuilder visit(Constant constant) {
    return null;
  }

  @Override
  public QueryBuilders.QueryBuilder visit(Path path) {
    return null;
  }

  private static class ConstantVisitor implements ExpressionVisitor<Object> {

    @Override
    public Object visit(Call call) {
      throw new UnsupportedOperationException("Expected a constant");
    }

    @Override
    public Object visit(Constant constant) {
      return constant.value();
    }

    @Override
    public Object visit(Path path) {
      throw new UnsupportedOperationException("Expected a constant");
    }
  }

  private static class PathVisitor implements ExpressionVisitor<String> {

    @Override
    public String visit(Call call) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String visit(Constant constant) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String visit(Path path) {
      return path.toStringPath();
    }
  }
}
