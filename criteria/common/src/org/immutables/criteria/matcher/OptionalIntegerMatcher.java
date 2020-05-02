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
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Intersection type between {@link OptionalObjectMatcher} and {@link NumberMatcher}.
 *
 * <p>Syntax sugar to avoid chaining {@code value()} method from {@link OptionalObjectMatcher}
 * on long expressions with many optional elements.
 *
 * @param <R> root criteria type
 */
public interface OptionalIntegerMatcher<R> extends OptionalNumberMatcher<R, Integer> {

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  default R is(OptionalInt optional) {
    Objects.requireNonNull(optional, "optional");
    return optional.isPresent() ? is(optional.getAsInt()) : isAbsent();
  }

  /**
   * Self-type for this matcher
   */
  interface Self extends Template<Self, Void>, Disjunction<Template<Self, Void>> {}

  interface Template<R, P> extends OptionalIntegerMatcher<R>, WithMatcher<R, Self>,
          NotMatcher<R, Self>, Projection<P>, Aggregation.NumberTemplate<OptionalInt, OptionalLong, OptionalDouble> {}

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
