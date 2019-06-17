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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A set of predefined utilities and factories for expressions like {@link Constant} or {@link Call}
 */
public final class Expressions {

  private Expressions() {}

  public static Constant constant(final Object value) {
    return Constant.of(value);
  }

  public static Expression and(Expression first, Expression second) {
    return and(Arrays.asList(first, second));
  }

  public static  Expression and(Iterable<? extends Expression> expressions) {
    return reduce(Operators.AND, expressions);
  }

  public static  Expression or(Expression first, Expression second) {
    return or(Arrays.asList(first ,second));
  }

  public static  Expression or(Iterable<? extends Expression> expressions) {
    return reduce(Operators.OR, expressions);
  }

  public static Root root(Class<?> entityClass) {
    return Root.of(entityClass);
  }

  private static  Expression reduce(Operator operator, Iterable<? extends Expression> expressions) {
    final int size = Iterables.size(expressions);

    if (size == 0) {
      throw new IllegalArgumentException("Empty iterator");
    } else if (size == 1) {
      return expressions.iterator().next();
    }

    return call(operator, expressions);
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

      @Override
      public V visit(Root root, @Nullable Void context) {
        return visitor.visit(root);
      }
    };
  }

  public static Optional<Expression> extractPredicate(Expression expression) {
    if (expression instanceof Root) {
      return ((Root) expression).expression();
    }

    return Optional.of(expression);
  }

  public static Expression not(Expression call) {
    if (call instanceof Root) {
      return ((Root) call).transform(e -> call(Operators.NOT, e));
    }
    return Expressions.call(Operators.NOT, call);
  }

  public static Call call(final Operator operator, Expression ... operands) {
    return call(operator, ImmutableList.copyOf(operands));
  }

  public static  Call call(final Operator operator, final Iterable<? extends Expression> operands) {
    final List<Expression> ops = ImmutableList.copyOf(operands);
    return new Call() {
      @Override
      public List<Expression> arguments() {
        return ops;
      }

      @Override
      public Operator operator() {
        return operator;
      }

      @Nullable
      @Override
      public <R, C> R accept(ExpressionBiVisitor<R, C> visitor, @Nullable C context) {
        return visitor.visit(this, context);
      }
    };
  }

}
