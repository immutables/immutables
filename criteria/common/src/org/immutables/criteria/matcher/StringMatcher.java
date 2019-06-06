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

/**
 * String specific criterias like {@code startsWith}, {@code contains} etc.
 */
public interface StringMatcher<R> extends ComparableMatcher<R, String>, NotMatcher<R, StringMatcher<StringMatcher.Self>> {

  default R isEmpty() {
    return Matchers.extract(this).<R>factory1().create1(e -> Expressions.call(Operators.EQUAL, e, Expressions.constant("")));
  }

  default R isNotEmpty() {
    return Matchers.extract(this).<R>factory1().create1(e -> Expressions.call(Operators.NOT_EQUAL, e, Expressions.constant("")));
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

  default R hasSize(int size) {
    throw new UnsupportedOperationException();
  }

  interface Self extends StringMatcher<Self>, Disjunction<StringMatcher<Self>> {

    @Override
    default StringMatcher<StringMatcher.Self> or() {
      return Matchers.extract(this).or().<StringMatcher.Self>factory1().create1();
    }
  }

}
