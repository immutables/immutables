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

package org.immutables.criteria.mongo;

import com.google.common.base.Preconditions;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.immutables.criteria.expression.AbstractExpressionVisitor;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Visitors;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates mongo find using visitor API.
 */
class MongoQueryVisitor extends AbstractExpressionVisitor<Bson> {

  MongoQueryVisitor() {
    super(e -> { throw new UnsupportedOperationException(); });
  }

  @Override
  public Bson visit(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = Visitors.toPath(args.get(0)).toStringPath();
      final Object value = Visitors.toConstant(args.get(1)).value();

      return op == Operators.EQUAL ? Filters.eq(field, value) : Filters.ne(field, value);
    }

    if (op == Operators.IN || op == Operators.NOT_IN) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      final String field = Visitors.toPath(args.get(0)).toStringPath();
      @SuppressWarnings("unchecked")
      final Iterable<Object> values = (Iterable<Object>) Visitors.toConstant(args.get(1)).value();
      Preconditions.checkNotNull(values, "not expected to be null %s", args.get(1));

      return op == Operators.IN ? Filters.in(field, values) : Filters.nin(field, values);
    }


    if (op == Operators.AND || op == Operators.OR) {
      final List<Bson> list = call.arguments().stream()
              .map(a -> a.accept(this))
              .collect(Collectors.toList());

      return op == Operators.AND ? Filters.and(list) : Filters.or(list);
    }

    throw new UnsupportedOperationException(String.format("Not yet supported (%s): %s", call.operator(), call));
  }


}
