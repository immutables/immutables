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

import java.util.regex.Pattern;

/**
 * String specific criterias like {@code startsWith}, {@code contains} etc.
 */
public interface StringMatcher<R> extends ComparableMatcher<R, String>  {

  default R isEmpty() {
    return isEqualTo("");
  }

  default R isNotEmpty() {
    return isNotEqualTo("");
  }

  default R contains(CharSequence other) {
    throw new UnsupportedOperationException();
  }

  default R startsWith(CharSequence prefix) {
    throw new UnsupportedOperationException();
  }

  default R endsWith(CharSequence suffix) {
    throw new UnsupportedOperationException();
  }

  /**
   * Checks wherever string matches regular expression
   */
  default R matches(Pattern regex) {
    throw new UnsupportedOperationException();
  }

  /**
   * Matches length of a string
   */
  default R hasLength(int size) {
    throw new UnsupportedOperationException();
  }

  interface Self extends Template<Self>, Disjunction<StringMatcher<Self>> {

    @Override
    default StringMatcher<StringMatcher.Self> or() {
      return Matchers.extract(this).or().<StringMatcher.Self, Object>factory().createRoot();
    }
  }

  interface With<R> extends WithMatcher<R, Self> {}

  interface Not<R> extends NotMatcher<R, Self> {}

  interface Template<R> extends StringMatcher<R>, With<R>, Not<R> {}

}
