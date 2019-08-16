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

public enum Operators implements Operator {

  /**
   * One element equal ({@code =}) to another (object equality)
   */
  EQUAL(Arity.BINARY),

  /**
   * One element NOT equal ({@code !=}) to another (object equality)
   */
  NOT_EQUAL(Arity.BINARY),

  /**
   * Element present in a collection
   */
  IN(Arity.BINARY),

  /**
   * Element NOT present in a collection
   */
  NOT_IN(Arity.BINARY),

  // boolean ops
  AND(Arity.BINARY),
  OR(Arity.BINARY),
  NOT(Arity.UNARY);

  private final Arity arity;

  Operators(Arity arity) {
    this.arity = arity;
  }

  @Override
  public Arity arity() {
    return arity;
  }
}
