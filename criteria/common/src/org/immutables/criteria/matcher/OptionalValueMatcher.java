/*
 * Copyright 2020 Immutables Authors and Contributors
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
 * Optional matcher for scalar (string, int, comparable etc.) attributes.
 * In addition to standard {@link ObjectMatcher} contract offers similar methods but with
 * {@link Optional} arguments. Depending on optional value (empty / present) matcher will
 * delegate to {@link #isPresent()}, {@link #isAbsent()} or {@link #is(Object)}
 */
public interface OptionalValueMatcher<R, V> extends ObjectMatcher<R, V>, PresentAbsentMatcher<R> {

  /**
   * Match current attribute given an optional parameter. If optional is
   * empty, matching is equivalent to {@link #isAbsent()} otherwise standard
   * {@link #is(Object)} matching is used.
   *
   * @param optional argument to match with
   * @throws NullPointerException if argument is null
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  default R is(Optional<? extends V> optional) {
    Objects.requireNonNull(optional, "optional");
    return optional.map(this::is).orElseGet(this::isAbsent);
  }

  /**
   * Match current attribute given an optional parameter. If optional is
   * empty, matching is equivalent to {@link #isPresent()} ()} otherwise standard
   * {@link #isNot(Object)} matching is used.
   *
   * <p><strong>Note</strong> Different backends might treat null (missing/unknown) value differently. Negations
   * might or might not return missing value.
   * For SQL there is <a href="https://modern-sql.com/concept/three-valued-logic">Three-Valued-Logic</a>.
   * while Mongo is two-valued.
   * @throws NullPointerException if argument is null
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  default R isNot(Optional<? extends V> optional) {
    Objects.requireNonNull(optional, "optional");
    return optional.map(this::isNot).orElseGet(this::isPresent);
  }

}
