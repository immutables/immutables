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

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Utilities to simplify visiting {@link ExpressionVisitor} and processing an {@link Expression}.
 */
public final class Visitors {

  private Visitors() {}

  /**
   * Function which returns an exception (to be thrown) when an expressin does not match expected type
   */
  private static final BiFunction<? super Expression, Class<? extends Expression>, IllegalArgumentException> ERROR_FN =
          (e, type) -> new IllegalArgumentException(String.format("Expression %s is not of type %s", e, type.getSimpleName()));


  /**
   * If {@code expression} is {@link Path} returns optional describing found constant
   * otherwise it is an empty optional.
   */
  public static Optional<Path> maybePath(Expression expression) {
    Objects.requireNonNull(expression, "expression");
    return expression.accept(new AbstractExpressionVisitor<Optional<Path>>(Optional.empty()) {
      @Override
      public Optional<Path> visit(Path path) {
        return Optional.of(path);
      }
    });
  }

  /**
   * Assumes current expression is a {@link Path}. Throws exception when it is not.
   */
  public static Path toPath(Expression expression) {
    return maybePath(expression).orElseThrow(() -> ERROR_FN.apply(expression, Path.class));
  }

  /**
   * If {@code expression} is {@link Constant} returns optional describing found constant
   * otherwise it is an empty optional.
   */
  public static Optional<Constant> maybeConstant(Expression expression) {
    Objects.requireNonNull(expression, "expression");
    return expression.accept(new AbstractExpressionVisitor<Optional<Constant>>(Optional.empty()) {
      @Override
      public Optional<Constant> visit(Constant constant) {
        return Optional.of(constant);
      }
    });
  };

  /**
   * Assumes current expression is a {@link Constant}. Throws exception when it is not.
   */
  public static Constant toConstant(Expression expression) {
    return maybeConstant(expression)
            .orElseThrow(() -> ERROR_FN.apply(expression, Constant.class));
  }

  public static Optional<Call> maybeCall(Expression expression) {
    Objects.requireNonNull(expression, "expression");
    return expression.accept(new AbstractExpressionVisitor<Optional<Call>>(Optional.empty()) {
      @Override
      public Optional<Call> visit(Call call) {
        return Optional.of(call);
      }
    });
  }

  public static Call toCall(Expression expression) {
    Objects.requireNonNull(expression, "expression");
    return maybeCall(expression).orElseThrow(() -> ERROR_FN.apply(expression, Call.class));
  }

  public static boolean isAggregationCall(Expression expression) {
    return maybeCall(expression)
            .map(c -> AggregationOperators.isAggregation(c.operator()))
            .orElse(false);
  }

}
