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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.immutables.criteria.DocumentCriteria;
import org.immutables.criteria.ValueCriteria;

import java.util.ArrayList;
import java.util.List;

/**
 * Comparing directly values of an attribute.
 *
 * @param <V> attribute type for which criteria is applied
 * @param <C> Criteria self-type, allowing {@code this}-returning methods to avoid needing subclassing
 * @param <T> type of the document being evaluated by this criteria
 */
public class ObjectCriteria<V, C extends DocumentCriteria<C, T>, T> implements ValueCriteria<C, T> {

  final String name;
  final Constraints.Constraint constraint;
  final CriteriaCreator<C, T> creator;

  ObjectCriteria(String name, Constraints.Constraint constraint, CriteriaCreator<C, T> creator) {
    this.name = Preconditions.checkNotNull(name, "name");
    this.constraint = Preconditions.checkNotNull(constraint, "constraint");
    this.creator = Preconditions.checkNotNull(creator, "creator");
  }

  public C isEqualTo(V value) {
    return creator.create(constraint.equal(name, false, value));
  }

  public C isNotEqualTo(V value) {
    return creator.create(constraint.equal(name, true, value));
  }

  public C isIn(V v1, V v2, V ... rest) {
    final List<V> values = new ArrayList<>(2 + rest.length);
    values.add(v1);
    values.add(v2);
    for (V v: rest) {
      values.add(v);
    }

    return isIn(values);
  }

  public C isNotIn(V v1, V v2, V ... rest) {
    final List<V> values = new ArrayList<>(2 + rest.length);
    values.add(v1);
    values.add(v2);
    for (V v: rest) {
      values.add(v);
    }

    return isNotIn(values);
  }

  public C isIn(Iterable<? super V> values) {
    Preconditions.checkNotNull(values, "values");
    return creator.create(constraint.in(name, false, ImmutableList.copyOf(values)));
  }

  public C isNotIn(Iterable<? super V> values) {
    Preconditions.checkNotNull(values, "values");
    return creator.create(constraint.in(name, true, ImmutableList.copyOf(values)));
  }

}
