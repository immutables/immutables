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


import java.util.Objects;
import java.util.Optional;

/**
 * Intersection type between {@link OptionalValueMatcher} and {@link BooleanMatcher}
 *
 * @param <R> root criteria type
 */
public interface OptionalBooleanMatcher<R> extends BooleanMatcher<R>, PresentAbsentMatcher<R> {

  /**
   * Match current boolean attribute given an optional boolean parameter. If optional is
   * empty, matching is equivalent to {@link #isAbsent()} otherwise standard
   * {@link #is(boolean)} matching is used.
   *
   * @param optional argument to match with
   * @throws NullPointerException if argument is null
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  default R is(Optional<Boolean> optional) {
    Objects.requireNonNull(optional, "optional");
    return optional.map(this::is).orElseGet(this::isAbsent);
  }

  interface Self extends Template<Self, Void> {}

  interface Template<R, P> extends OptionalBooleanMatcher<R>, WithMatcher<R, Self>, NotMatcher<R, Self>, Projection<P>, Aggregation.Count {}

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
