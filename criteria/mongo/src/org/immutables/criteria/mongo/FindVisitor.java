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
import com.google.common.collect.ImmutableSet;
import com.mongodb.client.model.Filters;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.AbstractExpressionVisitor;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.ComparableOperators;
import org.immutables.criteria.expression.Constant;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.IterableOperators;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.OptionalOperators;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.StringOperators;
import org.immutables.criteria.expression.Visitors;

import java.util.ArrayList;
import java.util.Collection;
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
  private final CodecRegistry codecRegistry;

  FindVisitor(PathNaming pathNaming, CodecRegistry codecRegistry) {
    super(e -> { throw new UnsupportedOperationException(); });
    this.naming = Objects.requireNonNull(pathNaming, "pathNaming");
    this.codecRegistry = Objects.requireNonNull(codecRegistry, "codecRegistry");
  }

  @Override
  public Bson visit(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    if (op == OptionalOperators.IS_ABSENT || op == OptionalOperators.IS_PRESENT) {
      return handleOptionalOperator(op, args);
    }

    if (op == Operators.AND || op == Operators.OR) {
      return handleLogicalOperator(op, args);
    }

    if (op == Operators.NOT) {
      return negate(args.get(0));
    }

    if (op == IterableOperators.IS_EMPTY || op == IterableOperators.NOT_EMPTY) {
      return handleIterableOperator(op, args);
    }
    
    if (op == IterableOperators.ANY) {
      Call rightCall = call;
      Path path = null;

      while (rightCall.operator() == IterableOperators.ANY) {
        Preconditions.checkArgument(rightCall.arguments().size() == 2, "Expected two arguments for %s got %s", rightCall, rightCall.arguments().size());
        Preconditions.checkArgument(rightCall.arguments().get(0) instanceof Path, "%s is not a path", rightCall.arguments().get(0));
        Preconditions.checkArgument(rightCall.arguments().get(1) instanceof Call, "%s is not a call", rightCall.arguments().get(1));

        path = Path.combine(path, (Path) rightCall.arguments().get(0));
        rightCall = (Call) rightCall.arguments().get(1);
      }

      // Handle AND/OR/NOT operators within ANY (for compound conditions on array elements)
      if (rightCall.operator() == Operators.AND ||
          rightCall.operator() == Operators.OR ||
          rightCall.operator() == Operators.NOT) {
        final String arrayField = naming.name(path);
        Bson condition = buildElemMatchCondition(rightCall);
        return Filters.elemMatch(arrayField, condition);
      }

      // Handle simple comparison operators within ANY
      Preconditions.checkArgument(rightCall.arguments().get(0) instanceof Path,"%s is not a path", rightCall.arguments().get(0));

      // Reconstruct the full path by combining the array path with the condition path
      Path currentPath = Visitors.toPath(rightCall.arguments().get(0));
      List<Path> paths = new ArrayList<>();
      while (currentPath.parent().isPresent()) {
        paths.add(currentPath);
        currentPath = currentPath.parent().get();
      }
      Collections.reverse(paths);
      for (Path tmpPath : paths) {
        path = Path.combine(path, tmpPath);
      }

      return buildCondition(rightCall, rightCall.operator(), path);
    }

    if (op.arity() == Operator.Arity.BINARY) {
      return binaryCall(call);
    }

    throw new UnsupportedOperationException(String.format("Not yet supported (%s): %s", call.operator(), call));
  }

  private Bson handleOptionalOperator(Operator op, List<Expression> args) {
    Preconditions.checkArgument(args.size() == 1, "Size should be 1 for %s but was %s", op, args.size());
    final String field = naming.name(Visitors.toPath(args.get(0)));
    if (op == OptionalOperators.IS_PRESENT) {
      return Filters.and(Filters.exists(field), Filters.ne(field, null));
    } else {
      // Absent fields mean null or missing
      return Filters.or(Filters.exists(field, false), Filters.eq(field, null));
    }
  }

  private Bson handleLogicalOperator(Operator op, List<Expression> args) {
    final List<Bson> list = args.stream()
            .map(a -> a.accept(this))
            .collect(Collectors.toList());

    return op == Operators.AND ? Filters.and(list) : Filters.or(list);
  }

  private Bson handleIterableOperator(Operator op, List<Expression> args) {
    Preconditions.checkArgument(args.size() == 1, "Size should be 1 for %s but was %s", op, args.size());
    final String field = naming.name(Visitors.toPath(args.get(0)));
    return op == IterableOperators.IS_EMPTY ? Filters.eq(field, Collections.emptyList())
            : Filters.and(Filters.exists(field), Filters.ne(field, null), Filters.ne(field, Collections.emptyList()));
  }


  private Bson binaryCall(Call call) {
    Preconditions.checkArgument(call.operator().arity() == Operator.Arity.BINARY, "%s is not binary", call.operator());
    final Operator op = call.operator();
    Expression left = call.arguments().get(0);
    Expression right = call.arguments().get(1);

    if (!(left instanceof Path && right instanceof Constant)) {
      // special case when $expr has to be used
      return call.accept(new MongoExpr(naming, codecRegistry)).asDocument();
    }

    final String field = naming.name(Visitors.toPath(left));
    final Object value = Visitors.toConstant(right).value();
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
      final Collection<Object> values = ImmutableSet.copyOf(Visitors.toConstant(right).values());
      Preconditions.checkNotNull(values, "not expected to be null for %s", op);
      if (values.size() == 1) {
        // optimization: convert IN, NIN (where argument is a list with single element) into EQ / NE
        Operators newOperator = op == Operators.IN ? Operators.EQUAL : Operators.NOT_EQUAL;
        Call newCall = Expressions.binaryCall(newOperator, left, Expressions.constant(values.iterator().next()));
        return binaryCall(newCall);
      }
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
    Operator notOperator = notCall.operator();

    if (notOperator == Operators.NOT) {
      // NOT NOT a == a
      return notCall.arguments().get(0).accept(this);
    } else if (notOperator == Operators.EQUAL) {
      return newCall(notCall, Operators.NOT_EQUAL);
    } else if (notOperator == Operators.NOT_EQUAL) {
      return newCall(notCall, Operators.EQUAL);
    } else if (notOperator == Operators.IN) {
      return newCall(notCall, Operators.NOT_IN);
    } else if (notOperator == Operators.NOT_IN) {
      return newCall(notCall, Operators.IN);
    } else if (notOperator == Operators.OR) {
      return Filters.nor(notCall.arguments().stream().map(a -> a.accept(this)).collect(Collectors.toList()));
    } else if (notOperator == Operators.AND) {
      // NOT A and B == (NOT A) or (NOT B)
      return Filters.or(notCall.arguments().stream().map(this::negate).collect(Collectors.toList()));
    } else if (notOperator == OptionalOperators.IS_ABSENT || notOperator == OptionalOperators.IS_PRESENT) {
      Operator newOp = notOperator == OptionalOperators.IS_ABSENT ? OptionalOperators.IS_PRESENT : OptionalOperators.IS_ABSENT;
      return newCall(notCall, newOp);
    } else if (notOperator == IterableOperators.IS_EMPTY || notOperator == IterableOperators.NOT_EMPTY) {
      Operator newOp = notOperator == IterableOperators.IS_EMPTY ? IterableOperators.NOT_EMPTY : IterableOperators.IS_EMPTY;
      return newCall(notCall, newOp);
    }

    // don't really know how to negate here
    return Filters.not(notCall.accept(this));
  }

  private Bson newCall(Call existing, Operator newOperator) {
    return visit(Expressions.call(newOperator, existing.arguments()));
  }

  private static Operator negateOperator(Operator operator) {
    if (operator == Operators.EQUAL) {
      return Operators.NOT_EQUAL;
    } else if (operator == Operators.NOT_EQUAL) {
      return Operators.EQUAL;
    } else if (operator == Operators.IN) {
      return Operators.NOT_IN;
    } else if (operator == Operators.NOT_IN) {
      return Operators.IN;
    } else if (operator == OptionalOperators.IS_ABSENT) {
      return OptionalOperators.IS_PRESENT;
    } else if (operator == OptionalOperators.IS_PRESENT) {
      return OptionalOperators.IS_ABSENT;
    } else if (operator == IterableOperators.IS_EMPTY) {
      return IterableOperators.NOT_EMPTY;
    } else if (operator == IterableOperators.NOT_EMPTY) {
      return IterableOperators.IS_EMPTY;
    } else {
      throw new UnsupportedOperationException("No negation for " + operator + " defined");
    }
  }

  /**
   * Recursively builds a MongoDB filter condition for use inside $elemMatch.
   * Handles arbitrarily nested AND/OR/NOT operators by traversing the expression tree.
   *
   * @param expr the expression to convert (must be a Call)
   * @return a Bson filter suitable for use inside Filters.elemMatch()
   */
  private Bson buildElemMatchCondition(Expression expr) {
    if (!(expr instanceof Call)) {
      throw new IllegalArgumentException("Expected Call but got " + expr);
    }

    Call call = (Call) expr;
    Operator op = call.operator();

    // Handle AND/OR recursively
    if (op == Operators.AND || op == Operators.OR) {
      List<Bson> conditions = new ArrayList<>();

      for (Expression arg : call.arguments()) {
        Preconditions.checkArgument(arg instanceof Call, "Expected Call but got %s", arg);
        Call argCall = (Call) arg;

        // Check if this argument has nested AND/OR that needs recursive handling
        if (argCall.operator() == Operators.AND || argCall.operator() == Operators.OR) {
          // Recursively process nested AND/OR
          conditions.add(buildElemMatchCondition(arg));
        } else {
          // Handle NOT and leaf conditions
          conditions.add(buildLeafCondition(argCall));
        }
      }

      return op == Operators.AND ? Filters.and(conditions) : Filters.or(conditions);
    }

    // For top-level NOT or leaf conditions
    return buildLeafCondition(call);
  }

  /**
   * Builds a condition that may be wrapped in NOT operator(s).
   * <p>
   * Handles three cases:
   * <ul>
   *   <li>Leaf conditions (comparisons like EQUAL, GREATER_THAN, etc.)</li>
   *   <li>NOT-wrapped leaf conditions (unwraps NOT, unwraps nested ANY, negates operator)</li>
   *   <li>NOT-wrapped AND/OR (applies De Morgan's laws recursively)</li>
   * </ul>
   *
   * @param call the condition call to process
   * @return a Bson filter for this condition
   */
  private Bson buildLeafCondition(Call call) {
    Operator operator = call.operator();

    // Handle NOT operator by unwrapping and negating the inner operator
    if (operator == Operators.NOT) {
      Preconditions.checkArgument(call.arguments().get(0) instanceof Call, "%s is not a call", call.arguments().get(0));
      call = (Call) call.arguments().get(0);

      // Unwrap any nested ANY operators (e.g., pet.type.not(type -> type.is(...)))
      while (call.operator() == IterableOperators.ANY) {
        Preconditions.checkArgument(call.arguments().size() == 2, "ANY should have 2 arguments");
        Preconditions.checkArgument(call.arguments().get(1) instanceof Call, "Second argument of ANY should be a Call");
        call = (Call) call.arguments().get(1);
      }

      // If after unwrapping we find AND/OR, apply De Morgan's laws
      if (call.operator() == Operators.AND || call.operator() == Operators.OR) {
        return applyDeMorgansLaw(call);
      }

      operator = negateOperator(call.operator());
    } else {
      operator = call.operator();
    }

    // Extract and reconstruct the path
    Preconditions.checkArgument(call.arguments().get(0) instanceof Path, "%s is not a path", call.arguments().get(0));
    Path fullPath = reconstructFullPath(Visitors.toPath(call.arguments().get(0)));

    // Build the final condition
    return buildCondition(call, operator, fullPath);
  }

  /**
   * Applies De Morgan's laws to negate a logical operator (AND/OR).
   * <p>
   * Transformations:
   * <ul>
   *   <li>NOT(a AND b) = NOT(a) OR NOT(b)</li>
   *   <li>NOT(a OR b) = NOT(a) AND NOT(b)</li>
   * </ul>
   *
   * @param call an AND or OR call to be negated
   * @return the negated condition with flipped operator
   */
  private Bson applyDeMorgansLaw(Call call) {
    Operator op = call.operator();
    List<Bson> negatedConditions = new ArrayList<>();

    for (Expression arg : call.arguments()) {
      // Wrap each argument in NOT and process it
      Call negatedArg = Expressions.unaryCall(Operators.NOT, (Call) arg);
      negatedConditions.add(buildLeafCondition(negatedArg));
    }

    // Apply De Morgan's law: flip AND <-> OR
    return op == Operators.AND ? Filters.or(negatedConditions) : Filters.and(negatedConditions);
  }

  /**
   * Builds a MongoDB filter from a call, operator, and path.
   * Creates either a unary expression (using the call's original operator) or
   * a binary expression (using the provided operator, which may be negated).
   *
   * @param call the original call (used for arity and second argument if binary)
   * @param operator the operator to use (may be negated from call's operator)
   * @param path the path to use in the expression
   * @return a Bson filter for this condition
   */
  private Bson buildCondition(Call call, Operator operator, Path path) {
    if (call.operator().arity() == Operator.Arity.UNARY) {
      return visit(Expressions.unaryCall(call.operator(), path));
    } else {
      return binaryCall(Expressions.binaryCall(operator, path, call.arguments().get(1)));
    }
  }

  /**
   * Reconstructs a full path from a potentially partial path by walking up the parent chain.
   */
  private static Path reconstructFullPath(Path path) {
    List<Path> paths = new ArrayList<>();
    Path current = path;
    while (current.parent().isPresent()) {
      paths.add(current);
      current = current.parent().get();
    }
    Collections.reverse(paths);

    Path result = null;
    for (Path p : paths) {
      result = Path.combine(result, p);
    }
    return result;
  }

  /**
   * Visitor used when special {@code $expr} needs to be generated like {@code field1 == field2}
   * in mongo it would look like:
   *
   * <pre>
   *   {@code
   *     $expr: {
   *     $eq: [
   *       "$field1",
   *       "$field2"
   *     ]
   *   }
   *   }
   * </pre>
   * @see <a href="https://docs.mongodb.com/manual/reference/operator/query/expr/">$expr</a>
   */
  private static class MongoExpr extends AbstractExpressionVisitor<BsonValue> {
    private final PathNaming pathNaming;
    private final CodecRegistry codecRegistry;

    private MongoExpr(PathNaming pathNaming, CodecRegistry codecRegistry) {
      super(e -> { throw new UnsupportedOperationException(); });
      this.pathNaming = pathNaming;
      this.codecRegistry = codecRegistry;
    }

    @Override
    public BsonValue visit(Call call) {
      Operator op = call.operator();
      if (op.arity() == Operator.Arity.BINARY) {
        return visitBinary(call, call.arguments().get(0), call.arguments().get(1));
      }

      if (op.arity() == Operator.Arity.UNARY) {
        return visitUnary(call, call.arguments().get(0));
      }


      throw new UnsupportedOperationException("Don't know how to handle " + call);
    }

    private BsonValue visitBinary(Call call, Expression left, Expression right) {
      Operator op = call.operator();

      String mongoOp;
      if (op == Operators.EQUAL) {
        mongoOp = "$eq";
      } else if (op == Operators.NOT_EQUAL) {
        mongoOp = "$ne";
      } else if (op == Operators.IN) {
        mongoOp = "$in";
      } else if (op == Operators.NOT_IN) {
        mongoOp = "$in"; // will be wrapped in $not: {$not: {$in: ... }}
      } else {
        throw new UnsupportedOperationException(String.format("Unknown operator %s for call %s", op, call));
      }

      BsonArray args = new BsonArray();
      args.add(left.accept(this));
      args.add(right.accept(this));

      BsonDocument expr = new BsonDocument(mongoOp, args);
      if (op == Operators.NOT_IN) {
        // for aggregations $nin does not work
        // use {$not: {$in: ... }} instead
        expr = new BsonDocument("$not", expr);
      }

      return Filters.expr(expr).toBsonDocument(BsonDocument.class, codecRegistry);
    }

    private BsonValue visitUnary(Call call, Expression arg) {
      Operator op = call.operator();

      if (op == StringOperators.TO_LOWER_CASE || op == StringOperators.TO_UPPER_CASE) {
        String key = op == StringOperators.TO_LOWER_CASE ? "$toLower" : "$toUpper";
        BsonValue value = arg.accept(this);
        return new BsonDocument(key, value);
      }

      throw new UnsupportedOperationException("Unknown unary call " + call);
    }

    @Override
    public BsonValue visit(Path path) {
      // in mongo expressions fields are referenced as $field
      return new BsonString('$' + pathNaming.name(path));
    }

    @Override
    public BsonValue visit(Constant constant) {
      Object value = constant.value();
      if (value == null) {
        return BsonNull.VALUE;
      }

      if (value instanceof Iterable) {
        return Filters.in("ignore", (Iterable<?>) value)
                .toBsonDocument(BsonDocument.class, codecRegistry)
                .get("ignore").asDocument()
                .get("$in").asArray();
      }

      return Filters.eq("ignore", value)
              .toBsonDocument(BsonDocument.class, codecRegistry)
              .get("ignore");
    }
  }

}
