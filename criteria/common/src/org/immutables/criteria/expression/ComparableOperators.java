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

import java.util.Arrays;
import java.util.Objects;

/**
 * List of operator for {@link Comparable}
 */
public enum ComparableOperators implements Operator {

  GREATER_THAN(Arity.BINARY),

  GREATER_THAN_OR_EQUAL(Arity.BINARY),

  LESS_THAN(Arity.BINARY),

  LESS_THAN_OR_EQUAL(Arity.BINARY);

  private final Arity arity;

  ComparableOperators(Arity arity) {
    this.arity = arity;
  }

  @Override
  public Arity arity() {
    return arity;
  }

  public static boolean isComparable(Operator operator) {
    if (!(operator instanceof ComparableOperators)) {
      return false;
    }
    return Arrays.asList(values()).contains(operator);
  }

}
