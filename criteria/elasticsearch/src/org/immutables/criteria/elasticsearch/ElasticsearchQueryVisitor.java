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
import org.immutables.criteria.expression.ComparableOperators;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.OptionalOperators;
import org.immutables.criteria.expression.StringOperators;
import org.immutables.criteria.expression.Visitors;

import java.util.List;
import java.util.regex.Pattern;

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
      final List<Object> values = Visitors.toConstant(args.get(1)).values();

      QueryBuilders.QueryBuilder builder = QueryBuilders.termsQuery(field, values);

      if (op == Operators.NOT_IN) {
        builder = QueryBuilders.boolQuery().mustNot(builder);
      }

      return builder;
    }

    if (op == OptionalOperators.IS_PRESENT || op == OptionalOperators.IS_ABSENT) {
      final String field = Visitors.toPath(args.get(0)).toStringPath();

      QueryBuilders.QueryBuilder builder = QueryBuilders.existsQuery(field);

      if (op == OptionalOperators.IS_ABSENT) {
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

    if (ComparableOperators.isComparable(op)) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = Visitors.toPath(args.get(0)).toStringPath();
      final Object value = Visitors.toConstant(args.get(1)).value();
      final QueryBuilders.RangeQueryBuilder builder = QueryBuilders.rangeQuery(field);

      if (op == ComparableOperators.GREATER_THAN) {
        builder.gt(value);
      } else if (op == ComparableOperators.GREATER_THAN_OR_EQUAL) {
        builder.gte(value);
      } else if (op == ComparableOperators.LESS_THAN) {
        builder.lt(value);
      } else if (op == ComparableOperators.LESS_THAN_OR_EQUAL) {
        builder.lte(value);
      } else {
        throw new UnsupportedOperationException("Unknown comparison " + call);
      }

      return builder;
    }

    if (op == StringOperators.MATCHES) {
      // regex
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = Visitors.toPath(args.get(0)).toStringPath();
      final Object value = Visitors.toConstant(args.get(1)).value();
      Preconditions.checkArgument(value instanceof Pattern, "%s is not regex pattern", value);
      // In elastic / lucene, patterns match the entire string.
      return QueryBuilders.regexpQuery(field, ((Pattern) value).pattern());
    }

    if (op == StringOperators.STARTS_WITH) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = Visitors.toPath(args.get(0)).toStringPath();
      final Object value = Visitors.toConstant(args.get(1)).value();
      return QueryBuilders.prefixQuery(field, value.toString());
    }

    if (op == StringOperators.ENDS_WITH) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = Visitors.toPath(args.get(0)).toStringPath();
      final Object value = Visitors.toConstant(args.get(1)).value();
      return QueryBuilders.wildcardQuery(field, "*" + value.toString());
    }

    throw new UnsupportedOperationException("Don't know how to handle " + call);
  }

}
