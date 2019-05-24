package org.immutables.criteria.elasticsearch;

import com.google.common.base.Preconditions;
import org.immutables.criteria.constraints.Call;
import org.immutables.criteria.constraints.Expression;
import org.immutables.criteria.constraints.ExpressionVisitor;
import org.immutables.criteria.constraints.Literal;
import org.immutables.criteria.constraints.Operator;
import org.immutables.criteria.constraints.Operators;
import org.immutables.criteria.constraints.Path;

import java.util.List;

public class ElasticsearchQueryVisitor implements ExpressionVisitor<QueryBuilders.QueryBuilder> {


  @Override
  public QueryBuilders.QueryBuilder visit(Call call) {
    final Operator op = call.getOperator();
    final List<Expression> args = call.getArguments();

    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = args.get(0).accept(new PathVisitor());
      final Object right = args.get(1).accept(new LiteralVisitor());

      QueryBuilders.QueryBuilder builder = QueryBuilders.termQuery(field, right);
      if (op == Operators.NOT_EQUAL) {
        builder = QueryBuilders.boolQuery().mustNot(builder);
      }

      return builder;
    }

    throw new UnsupportedOperationException("Don't know how to handle " + call);
  }

  @Override
  public QueryBuilders.QueryBuilder visit(Literal literal) {
    return null;
  }

  @Override
  public QueryBuilders.QueryBuilder visit(Path path) {
    return null;
  }

  private static class LiteralVisitor implements ExpressionVisitor<Object> {

    @Override
    public Object visit(Call call) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object visit(Literal literal) {
      return literal.value();
    }

    @Override
    public Object visit(Path path) {
      throw new UnsupportedOperationException();
    }
  }

  private static class PathVisitor implements ExpressionVisitor<String> {

    @Override
    public String visit(Call call) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String visit(Literal literal) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String visit(Path path) {
      return path.toStringPath();
    }
  }
}
