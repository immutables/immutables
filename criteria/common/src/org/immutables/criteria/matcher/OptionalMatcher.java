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

/**
 * Matcher for optional attributes
 */
public interface OptionalMatcher<R, S> extends PresentAbsentMatcher<R>, Matcher {

  /**
   * Apply context-specific matcher if value is present
   */
  default S value() {
    // CriteriaContext.this.creator;
    return Matchers.extract(this).<S>create();
  }

  /**
   * Self-type for this matcher
   */
  interface Self<S> extends Template<Self<S>, S, Void>, Disjunction<Self<S>> {}

  interface Template<R, S, P> extends OptionalMatcher<R, S>, Projection<P>, Aggregation.Count {}

  @SuppressWarnings("unchecked")
  static <R> CriteriaCreator<R> creator() {
    class Local extends AbstractContextHolder implements Self {
      private Local(CriteriaContext context) {
        super(context);
      }
    }

    return ctx -> (R) new Local(ctx);
  }


}
