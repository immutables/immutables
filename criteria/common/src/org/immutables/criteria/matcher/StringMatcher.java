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
import org.immutables.criteria.expression.Operators;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * String specific predicates like {@code startsWith}, {@code contains} etc.
 * @param <R> root criteria type
 */
public interface StringMatcher<R> extends ComparableMatcher<R, String>  {

  /**
   * Check that attribute is an empty string
   */
  default R isEmpty() {
    return is("");
  }

  /**
   * Check that attribute is not an empty string
   */
  default R notEmpty() {
    return isNot("");
  }

  default R contains(CharSequence other) {
    throw new UnsupportedOperationException();
  }

  /**
   * Check that string attribute starts with {@code prefix} prefix
   */
  default R startsWith(CharSequence prefix) {
    Objects.requireNonNull(prefix, "prefix");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(Operators.STARTS_WITH, e, Expressions.constant(prefix.toString())));
  }

  /**
   * Check that string attribute ends with {@code suffix} suffix
   */
  default R endsWith(CharSequence suffix) {
    Objects.requireNonNull(suffix, "suffix");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(Operators.ENDS_WITH, e, Expressions.constant(suffix.toString())));
  }

  /**
   * Checks wherever string matches regular expression
   * @param regex pattern to match for
   */
  default R matches(Pattern regex) {
    Objects.requireNonNull(regex, "regexp");
    return Matchers.extract(this).applyAndCreateRoot(e -> Expressions.call(Operators.MATCHES, e, Expressions.constant(regex)));
  }

  /**
   * Predicate for length of a string
   */
  default R hasLength(int size) {
    throw new UnsupportedOperationException();
  }

  /**
   * Self-type for this matcher
   */
  interface Self extends Template<Self>, Disjunction<StringMatcher<Self>> {}

  interface Template<R> extends StringMatcher<R>, WithMatcher<R, Self>, NotMatcher<R, Self>{}

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
