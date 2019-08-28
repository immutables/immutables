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

import java.lang.reflect.Type;
import java.util.Arrays;

public enum AggregationOperators implements Operator {

  COUNT(Arity.UNARY),
  SUM(Arity.UNARY),
  AVG(Arity.UNARY),
  MIN(Arity.UNARY),
  MAX(Arity.UNARY);

  private final Arity arity;

  AggregationOperators(Arity arity) {
    this.arity = arity;
  }

  @Override
  public Arity arity() {
    return arity;
  }

  @Override
  public Type returnType() {
    return Number.class;
  }

  public static boolean isAggregation(Operator operator) {
    if (!(operator instanceof AggregationOperators)) {
      return false;
    }

    return Arrays.asList(values()).contains(operator);
  }

}
