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

package org.immutables.criteria.matcher;

import org.immutables.criteria.Criterion;
import org.immutables.criteria.expression.Operators;

/**
 * Combines matchers using logical {@code OR}
 * @param <R> root criteria type
 */
public interface OrMatcher<R extends Criterion<?>> extends Matcher {

  /**
   * Combine {@code this} and {@code other} expression (criteria / matcher) using logical {@code OR}
   * operator. Equivalent to {@code this OR other}.
   *
   * @param other other matcher
   * @return new root criteria with updated expression
   */
  @SuppressWarnings("unchecked")
  default R or(R other) {
    return Matchers.combine((R) this, other, Operators.OR);
  }

}
