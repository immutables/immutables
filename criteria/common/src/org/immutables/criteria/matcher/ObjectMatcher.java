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

import com.google.common.collect.ImmutableList;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Comparing directly values of an attribute.
 *
 * @param <V> attribute type for which criteria is applied
 * @param <R> Criteria self-type, allowing {@code this}-returning methods to avoid needing subclassing
 */
public interface ObjectMatcher<R, V> {


  default R isEqualTo(V value) {
    return Matchers.extract(this).<R>factory1().create1(e -> Expressions.call(Operators.EQUAL, e, Expressions.constant(value)));
  }

  default R isNotEqualTo(V value) {
    return Matchers.extract(this).<R>factory1().create1(e -> Expressions.call(Operators.NOT_EQUAL, e, Expressions.constant(value)));
  }

  default R isIn(V v1, V v2, V ... rest) {
    final List<V> values = new ArrayList<>(2 + rest.length);
    values.add(v1);
    values.add(v2);
    values.addAll(Arrays.asList(rest));

    return isIn(values);
  }

  default R isNotIn(V v1, V v2, V ... rest) {
    final List<V> values = new ArrayList<>(2 + rest.length);
    values.add(v1);
    values.add(v2);
    values.addAll(Arrays.asList(rest));

    return isNotIn(values);
  }

  default R isIn(Iterable<? super V> values) {
    Objects.requireNonNull(values, "values");
    return Matchers.extract(this).<R>factory1().create1(e -> Expressions.call(Operators.IN, e, Expressions.constant(ImmutableList.copyOf(values))));
  }

  default R isNotIn(Iterable<? super V> values) {
    Objects.requireNonNull(values, "values");
    return Matchers.extract(this).<R>factory1().create1(e -> Expressions.call(Operators.NOT_IN, e, Expressions.constant(ImmutableList.copyOf(values))));
  }

  interface Self<V> extends ObjectMatcher<Self<V>, V> {}

}
