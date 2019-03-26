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

package org.immutables.criteria.constraints;

import org.immutables.criteria.DocumentCriteria;

/**
 * String specific criterias like {@code isAbsent}, {@code contains} etc.
 */
public class StringCriteria<C extends DocumentCriteria<C, T>, T> extends ComparableCriteria<String, C, T> {

  public StringCriteria(CriteriaCreator<C, T> creator) {
    super(creator);
  }

  public C isEmpty() {
    return create(e -> Expressions.<T>call(Operators.EQUAL, e, Expressions.literal("")));
  }

  public C isNotEmpty() {
    return create(e -> Expressions.<T>call(Operators.NOT_EQUAL, e, Expressions.literal("")));
  }

  public C contains(CharSequence other) {
    throw new UnsupportedOperationException();
  }

  public C startsWith(CharSequence prefix) {
    throw new UnsupportedOperationException();
  }

  public C endsWith(CharSequence suffix) {
    throw new UnsupportedOperationException();
  }

}
