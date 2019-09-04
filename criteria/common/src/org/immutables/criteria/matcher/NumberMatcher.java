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
 * Matcher for numbers
 *
 * @param <R> root criteria type
 */
public interface NumberMatcher<R, V extends Number & Comparable<? super V>> extends ComparableMatcher<R, V>, OrderMatcher {

  /**
   * Self-type for this matcher
   */
  interface Self<V extends Number & Comparable<? super V>> extends Template<Self<V>, V>, Disjunction<Template<Self<V>, V>> {}

  interface Template<R, V extends Number & Comparable<? super V>> extends NumberMatcher<R, V>, WithMatcher<R, Self<V>>,
          NotMatcher<R, Self<V>>,
          Projection<V>, AggregationTemplate<V> {}

  interface AggregationTemplate<V> extends Aggregation.NumberTemplate<V, Double, Double> {}

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
