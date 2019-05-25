/*
   Copyright 2013-2018 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package org.immutables.criteria.matcher;

import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operators;

/**
 * String specific criterias like {@code isAbsent}, {@code contains} etc.
 */
public class StringMatcher<R> extends ComparableMatcher<R, String> {

  public StringMatcher(CriteriaContext<R> context) {
    super(context);
  }

  public R isEmpty() {
    return create(e -> Expressions.call(Operators.EQUAL, e, Expressions.constant("")));
  }

  public R isNotEmpty() {
    return create(e -> Expressions.call(Operators.NOT_EQUAL, e, Expressions.constant("")));
  }

  public R contains(CharSequence other) {
    throw new UnsupportedOperationException();
  }

  public R startsWith(CharSequence prefix) {
    throw new UnsupportedOperationException();
  }

  public R endsWith(CharSequence suffix) {
    throw new UnsupportedOperationException();
  }

  public R hasSize(int size) {
    throw new UnsupportedOperationException();
  }

  public static class Self extends StringMatcher<Self> implements Disjunction<StringMatcher<Self>> {
    public Self(CriteriaContext<StringMatcher.Self> context) {
      super(context);
    }

    @Override
    public StringMatcher<StringMatcher.Self> or() {
      return context.or().create();
    }
  }

}
