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

package org.immutables.criteria;

import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressional;

import java.util.Objects;

public final class Criterias {

  private Criterias() {}

  /**
   * Extracts {@link Expressional} interface from a criteria. Any criteria implements
   * {@code Expressional} interface at runtime.
   */
  public static Expressional toExpressional(DocumentCriteria<?> criteria) {
    Objects.requireNonNull(criteria, "criteria");
    return (Expressional) criteria;
  }

  /**
   * Extract directly expression from a criteria
   */
  public static Expression toExpression(DocumentCriteria<?> criteria) {
    return toExpressional(criteria).expression();
  }

}
