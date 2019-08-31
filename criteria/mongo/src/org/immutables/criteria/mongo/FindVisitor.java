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
import org.bson.Document;
import org.bson.conversions.Bson;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.AbstractExpressionVisitor;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.ComparableOperators;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.IterableOperators;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.OptionalOperators;
import org.immutables.criteria.expression.StringOperators;
import org.immutables.criteria.expression.Visitors;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Converts find expression to Mongo BSON format
 * @see <a href="https://docs.mongodb.com/manual/tutorial/query-documents/">Query Documents</a>
 */
class FindVisitor extends AbstractExpressionVisitor<Bson> {

  private final PathNaming naming;

  FindVisitor(PathNaming naming) {
    super(e -> { throw new UnsupportedOperationException(); });
    this.naming = Objects.requireNonNull(naming, "pathNaming");
  }

  @Override
  public Bson visit(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();


    if (op == OptionalOperators.IS_ABSENT || op == OptionalOperators.IS_PRESENT) {
      Preconditions.checkArgument(args.size() == 1, "Size should be 1 for %s but was %s", op, args.size());
      final String field = naming.name(Visitors.toPath(args.get(0)));
      final Bson exists = Filters.and(Filters.exists(field), Filters.ne(field, null));
      return op == OptionalOperators.IS_PRESENT ? exists : Filters.not(exists);
    }

    if (op == Operators.AND || op == Operators.OR) {
      final List<Bson> list = call.arguments().stream()
              .map(a -> a.accept(this))
              .collect(Collectors.toList());

      return op == Operators.AND ? Filters.and(list) : Filters.or(list);
    }

    if (op == Operators.NOT) {
      return negate(args.get(0));
    }

    if (op == IterableOperators.IS_EMPTY || op == IterableOperators.NOT_EMPTY) {
      Preconditions.checkArgument(args.size() == 1, "Size should be 1 for %s but was %s", op, args.size());
      final String field = naming.name(Visitors.toPath(args.get(0)));
      return op == IterableOperators.IS_EMPTY ? Filters.eq(field, Collections.emptyList())
              : Filters.and(Filters.exists(field), Filters.nin(field, Collections.emptyList(), null));
    }

    if (op.arity() == Operator.Arity.BINARY) {
      return binaryCall(call);
    }


    throw new UnsupportedOperationException(String.format("Not yet supported (%s): %s", call.operator(), call));
  }

  private Bson binaryCall(Call call) {
    Preconditions.checkArgument(call.operator().arity() == Operator.Arity.BINARY, "%s is not binary", call.operator());
    final Operator op = call.operator();
    final String field = naming.name(Visitors.toPath(call.arguments().get(0)));
    final Object value = Visitors.toConstant(call.arguments().get(1)).value();
    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
      if ("".equals(value) && op == Operators.NOT_EQUAL) {
        // special case for empty string. string != "" should not return missing strings
        return Filters.and(Filters.nin(field, value, null), Filters.exists(field));
      }
      return op == Operators.EQUAL ? Filters.eq(field, value) : Filters.ne(field, value);
    }

    if (ComparableOperators.isComparable(op)) {
      if (op == ComparableOperators.GREATER_THAN) {
        return Filters.gt(field, value);
      } else if (op == ComparableOperators.GREATER_THAN_OR_EQUAL) {
        return Filters.gte(field, value);
      } else if (op == ComparableOperators.LESS_THAN) {
        return Filters.lt(field, value);
      } else if (op == ComparableOperators.LESS_THAN_OR_EQUAL) {
        return Filters.lte(field, value);
      }

      throw new UnsupportedOperationException("Unknown comparison " + call);
    }

    if (op == Operators.IN || op == Operators.NOT_IN) {
      final List<Object> values = Visitors.toConstant(call.arguments().get(1)).values();
      Preconditions.checkNotNull(values, "not expected to be null for %s", op);
      return op == Operators.IN ? Filters.in(field, values) : Filters.nin(field, values);
    }

    if (op == StringOperators.MATCHES || op == StringOperators.CONTAINS) {
      Object newValue = value;
      if (op == StringOperators.CONTAINS) {
        // handle special case for string contains with regexp
        newValue = Pattern.compile(".*" + Pattern.quote(value.toString()) + ".*");
      }
      Preconditions.checkArgument(newValue instanceof Pattern, "%s is not regex pattern", value);
      return Filters.regex(field, (Pattern) newValue);
    }

    if (op == IterableOperators.HAS_SIZE) {
      Preconditions.checkArgument(value instanceof Number, "%s is not a number", value);
      int size = ((Number) value).intValue();
      return Filters.size(field, size);
    }

    if (op == IterableOperators.CONTAINS) {
      return Filters.eq(field, value);
    }

    if (op == StringOperators.HAS_LENGTH) {
      Preconditions.checkArgument(value instanceof Number, "%s is not a number", value);
      final int length = ((Number) value).intValue();
      // use strLenCP function
      // https://docs.mongodb.com/manual/reference/operator/aggregation/strLenCP/#exp._S_strLenCP
      final Bson lengthExpr  = Document.parse(String.format("{$expr:{$eq:[{$strLenCP: \"$%s\"}, %d]}}}", field, length));
      // field should exists and not be null
      return Filters.and(Filters.exists(field), Filters.ne(field, null), lengthExpr);
    }

    if (op == StringOperators.STARTS_WITH || op == StringOperators.ENDS_WITH) {
      // regular expression
      final String pattern = String.format("%s%s%s",
              op == StringOperators.STARTS_WITH ? "^": "",
              Pattern.quote(value.toString()),
              op == StringOperators.ENDS_WITH ? "$" : "");
      return Filters.regex(field, Pattern.compile(pattern));
    }

    throw new UnsupportedOperationException(String.format("Unsupported binary call %s", call));
  }

  /**
   * see https://docs.mongodb.com/manual/reference/operator/query/not/
   * NOT operator is mongo is a little specific and can't be applied on all levels
   * $not: { $or ... } will not work and should be transformed to $nor
   */
  private Bson negate(Expression expression) {
    if (!(expression instanceof Call)) {
      return Filters.not(expression.accept(this));
    }

    Call notCall = (Call) expression;

    if (notCall.operator() == Operators.NOT) {
      // NOT NOT a == a
      return notCall.arguments().get(0).accept(this);
    } else if (notCall.operator() == Operators.EQUAL) {
      return visit(Expressions.call(Operators.NOT_EQUAL, notCall.arguments()));
    } else if (notCall.operator() == Operators.NOT_EQUAL) {
      return visit(Expressions.call(Operators.EQUAL, notCall.arguments()));
    } else if (notCall.operator() == Operators.IN) {
      return visit(Expressions.call(Operators.NOT_IN, notCall.arguments()));
    } else if (notCall.operator() == Operators.NOT_IN) {
      return visit(Expressions.call(Operators.IN, notCall.arguments()));
    } else if (notCall.operator() == Operators.OR) {
      return Filters.nor(notCall.arguments().stream().map(a -> a.accept(this)).collect(Collectors.toList()));
    } else if (notCall.operator() == Operators.AND) {
      // NOT A and B == (NOT A) or (NOT B)
      return Filters.or(notCall.arguments().stream().map(this::negate).collect(Collectors.toList()));
    }

    // don't really know how to negate here
    return Filters.not(notCall.accept(this));
  }

}
