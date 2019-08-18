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

import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.StringOperators;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * String specific predicates like {@code startsWith}, {@code contains} etc.
 * @param <R> root criteria type
 */
public interface StringMatcher<R> extends ComparableMatcher<R, String> {

  /**
   * Check for empty string (equivalent to {@code string.length() == 0})
   */
  default R isEmpty() {
    return is("");
  }

  /**
   * Check for NON-empty string (equivalent to {@code string.length() > 0})
   */
  default R notEmpty() {
    return isNot("");
  }

  /**
   *  Check that attribute contains {@code other}
   */
  default R contains(CharSequence other) {
    Objects.requireNonNull(other, "other");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(StringOperators.CONTAINS, e, Expressions.constant(other.toString())));
  }

  /**
   * Check that string attribute starts with {@code prefix} prefix
   */
  default R startsWith(CharSequence prefix) {
    Objects.requireNonNull(prefix, "prefix");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(StringOperators.STARTS_WITH, e, Expressions.constant(prefix.toString())));
  }

  /**
   * Check that string attribute ends with {@code suffix} suffix
   */
  default R endsWith(CharSequence suffix) {
    Objects.requireNonNull(suffix, "suffix");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(StringOperators.ENDS_WITH, e, Expressions.constant(suffix.toString())));
  }

  /**
   * Checks wherever string matches regular expression
   * @param regex pattern to match for
   */
  default R matches(Pattern regex) {
    Objects.requireNonNull(regex, "regexp");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(StringOperators.MATCHES, e, Expressions.constant(regex)));
  }

  /**
   * Predicate for length of a string
   */
  default R hasLength(int length) {
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(StringOperators.HAS_LENGTH, e, Expressions.constant(length)));
  }

  /**
   * Self-type for this matcher
   */
  interface Self extends Template<Self>, Disjunction<StringMatcher<Self>> {}

  interface Template<R> extends StringMatcher<R>, WithMatcher<R, Self>, NotMatcher<R, Self>, Projection<String> {}

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
