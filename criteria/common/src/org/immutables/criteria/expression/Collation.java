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

/**
 * Definition of an ordering. Pretty much encoded {@code ORDER BY} clause.
 */
public class Collation implements Ordering {

  private final Expression expression;

  private final Direction direction;

  private Collation(Expression expression, Direction direction) {
    this.expression = Objects.requireNonNull(expression, "expression");
    this.direction = Objects.requireNonNull(direction, "direction");
  }

  public Expression expression() {
    return expression;
  }

  public Path path() {
    return (Path) expression();
  }

  public Direction direction() {
    return direction;
  }

  public static Collation of(Expression expression, Direction direction) {
    return new Collation(expression, direction);
  }

  public static Collation of(Expression expression) {
    return of(expression, Direction.ASCENDING);
  }

}
