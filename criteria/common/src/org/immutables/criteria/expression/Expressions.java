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

package org.immutables.criteria.expression;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A set of predefined utilities and factories for expressions like {@link Constant} or {@link Call}
 */
public final class Expressions {

  private Expressions() {}

  public static Constant constant(Object value) {
    if (value == null) {
      throw new NullPointerException(String.format("value argument is null. Use method %s.constantOfType(Object, Type)", Expressions.class.getSimpleName()));
    }
    return constantOfType(value, value.getClass());
  }

  public static Constant constantOfType(@Nullable Object value, Type type) {
    Objects.requireNonNull(type, "type");
    return Constant.ofType(value, type);
  }

  public static Call and(Expression left, Expression right) {
    return and(ImmutableList.of(left, right));
  }

  public static Call and(Iterable<? extends Expression> expressions) {
    List<Expression> args = ImmutableList.copyOf(expressions);
    Preconditions.checkArgument(args.size() >= 2, "expected at least 2 arguments for %s but got %s", Operators.AND, args.size());
    return call(Operators.AND, expressions);
  }

  public static Call or(Expression first, Expression second) {
    return or(ImmutableList.of(first, second));
  }

  public static Call or(Iterable<? extends Expression> expressions) {
    List<Expression> args = ImmutableList.copyOf(expressions);
    Preconditions.checkArgument(args.size() >= 2, "expected at least 2 arguments for %s but got %s", Operators.OR, args.size());
    return call(Operators.OR, expressions);
  }

  public static Expression aggregation(AggregationOperators operator, Type returnType, Expression expression) {
    return ImmutableCall.of(Collections.singletonList(expression), operator, returnType);
  }

  /**
   * Converts a {@link ExpressionVisitor} into a {@link ExpressionBiVisitor} (with ignored payload).
   */
  static <V> ExpressionBiVisitor<V, Void> toBiVisitor(ExpressionVisitor<V> visitor) {
    return new ExpressionBiVisitor<V, Void>() {
      @Override
      public V visit(Call call, @Nullable Void context) {
        return visitor.visit(call);
      }

      @Override
      public V visit(Constant constant, @Nullable Void context) {
        return visitor.visit(constant);
      }

      @Override
      public V visit(Path path, @Nullable Void context) {
        return visitor.visit(path);
      }

    };
  }

  public static Call not(Expression call) {
    return unaryCall(Operators.NOT, call);
  }

  public static Call unaryCall(Operator operator, Expression arg) {
    Objects.requireNonNull(arg, "arg");
    return call(operator, ImmutableList.of(arg));
  }

  public static Call binaryCall(final Operator operator, Expression left, Expression right) {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");
    return call(operator, ImmutableList.of(left, right));
  }

  /**
   * Create generic call with given arguments
   *
   * @deprecated prefer using {@link #call(Operator, Iterable)} iterable variant of this function
   *             or more specific {@link #binaryCall(Operator, Expression, Expression)}
   */
  @Deprecated
  public static Call call(final Operator operator, Expression ... operands) {
    return call(operator, ImmutableList.copyOf(operands));
  }

  public static Call call(final Operator operator, final Iterable<? extends Expression> operands) {
    return Call.of(operator, operands);
  }

}
