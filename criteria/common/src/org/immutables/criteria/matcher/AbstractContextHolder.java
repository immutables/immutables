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

import org.immutables.criteria.expression.Query;

import java.util.Objects;

/**
 * Exposes context of a matcher / criteria. Context is similar to "state".
 * Used as private API (not visible in regular API).
 *
 * @see org.immutables.criteria.Criterion
 * @see Matcher
 */
public abstract class AbstractContextHolder {

  private final CriteriaContext context;

  protected AbstractContextHolder(CriteriaContext context) {
    this.context = Objects.requireNonNull(context, "context");
  }

  /**
   * Expose current context of the matcher. This method is used to extract and combine different
   * expressions (matchers).
   */
  CriteriaContext context() {
    return context;
  }

  Query query() {
    return context().query();
  }

}
