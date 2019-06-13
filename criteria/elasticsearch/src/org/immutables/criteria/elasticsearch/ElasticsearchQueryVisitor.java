/*
 * Copyright 2019 Immutables Authors and Contributors
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

package org.immutables.criteria.elasticsearch;

import com.google.common.base.Preconditions;
import org.immutables.criteria.expression.AbstractExpressionVisitor;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.OperatorTables;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Visitors;

import java.util.List;

/**
 * Evaluating expression into <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html">Elastic Query DSL</a>
 */
class ElasticsearchQueryVisitor extends AbstractExpressionVisitor<QueryBuilders.QueryBuilder> {

  ElasticsearchQueryVisitor() {
    super(e -> { throw new UnsupportedOperationException(); });
  }

  @Override
  public QueryBuilders.QueryBuilder visit(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = Visitors.toPath(args.get(0)).toStringPath();
      final Object right = Visitors.toConstant(args.get(1)).value();

      QueryBuilders.QueryBuilder builder = QueryBuilders.termQuery(field, right);
      if (op == Operators.NOT_EQUAL) {
        builder = QueryBuilders.boolQuery().mustNot(builder);
      }

      return builder;
    }

    if (op == Operators.IN || op == Operators.NOT_IN) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = Visitors.toPath(args.get(0)).toStringPath();
      @SuppressWarnings("unchecked")
      final Iterable<Object> values = (Iterable<Object>) Visitors.toConstant(args.get(1)).value();
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
      final String field = Visitors.toPath(args.get(0)).toStringPath();
      final Object value = Visitors.toConstant(args.get(1)).value();
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

}
