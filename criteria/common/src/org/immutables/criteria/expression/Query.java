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

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Main criteria expression (always present as root element). Predicate
 * is sub-element of this expression.
 */
public final class Query implements Expression {

  private final EntityPath entityPath;
  private final Expression expression;

  private Query(EntityPath entityPath, Expression expression) {
    this.entityPath = Objects.requireNonNull(entityPath, "entityPath");
    this.expression = expression;
  }

  @Nullable
  @Override
  public <R, C> R accept(ExpressionBiVisitor<R, C> visitor, @Nullable C context) {
    return visitor.visit(this, context);
  }

  public EntityPath entityPath() {
    return this.entityPath;
  }

  public Optional<Expression> expression() {
    return Optional.ofNullable(expression);
  }

  static Query of(Class<?> entityClass) {
    return new Query(EntityPath.of(entityClass), null);
  }

  Query withExpression(Expression expression) {
    Objects.requireNonNull(expression, "expression");
    return new Query(entityPath, expression);
  }

  public Query transform(UnaryOperator<Expression> operator) {
    return expression().map(e -> withExpression(operator.apply(e))).orElse(this);
  }

}
