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
 * String specific criterias like {@code isEmpty}, {@code contains} etc.
 */
public class StringCriteria<C extends DocumentCriteria<C, T>, T> extends ComparableCriteria<String, C, T> {

  public StringCriteria(String name, Constraints.Constraint constraint, CriteriaCreator<C, T> creator) {
    super(name, constraint, creator);
  }

  public C isEmpty() {
    return creator.create(constraint.equal(name, false, ""));
  }

  public C isNotEmpty() {
    return creator.create(constraint.equal(name, true, ""));
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
