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

import java.util.List;

/**
 * An expression formed by a call to an operator (eg. {@link Operators#EQUAL}) with zero or more arguments.
 */
public interface Call extends Expression {

  /**
   * Get arguments of this operation
   */
  List<Expression> arguments();

  /**
   * Get the operator symbol for this operation
   *
   * @return operator
   */
  Operator operator();
}
