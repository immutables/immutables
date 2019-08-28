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

/**
 * Marker interface for now.
 * Operators can be {@code >}, {@code min}, {@code abs} etc.
 */
public interface Operator {

  /**
   * Name of the operator can be {@code =} or {@code <} etc.
   */
  String name();

  /**
   * Number of arguments for operator.
   * @see <a href="https://en.wikipedia.org/wiki/Arity">Arity</a>
   */
  Arity arity();

  /**
   * Default result type of applying this operator
   */
  Type returnType();

  /**
   * Arity is number of arguments current operator takes.
   * @see <a href="https://en.wikipedia.org/wiki/Arity">Arity</a>
   */
  enum Arity {
    /**
     * Single argument operator. Can be prefix or postfix. Example {@code NOT a}, {@code a++}, {@code -a}
     */
    UNARY,

    /**
     * Operator with two arguments. Example {@code a AND b}, {@code a + b}, {@code a > b}, {@code a in b}
     */
    BINARY,

    /**
     * Ternary operator with three arguments. Example {@code a ? b: c}
     */
    TERNARY
  }

}
