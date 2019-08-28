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

public enum StringOperators implements Operator {

  /**
   * Regexp matching
   * @see java.util.regex.Pattern
   */
  MATCHES(Arity.BINARY),

  /**
   * One string contains another
   */
  CONTAINS(Arity.BINARY),

  /**
   * String starts with a prefix
   */
  STARTS_WITH(Arity.BINARY),

  /**
   * String ends with a suffix
   */
  ENDS_WITH(Arity.BINARY),

  /**
   * String has particular length. Eg. {@code 0} for empty string.
   */
  HAS_LENGTH(Arity.BINARY);

  private final Arity arity;

  StringOperators(Arity arity) {
    this.arity = arity;
  }

  @Override
  public Arity arity() {
    return arity;
  }

  @Override
  public Type returnType() {
    return Boolean.class;
  }
}
