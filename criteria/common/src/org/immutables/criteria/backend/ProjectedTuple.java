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

package org.immutables.criteria.backend;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.immutables.criteria.expression.Expression;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of a projection as returned by the backend
 */
public class ProjectedTuple {

  private final List<Expression> expressions;
  private final List<?> values; // values can be null
  private final Map<Expression, Object> valuesAsMap;

  private ProjectedTuple(List<Expression> expressions, List<?> values) {
    if (values.size() != expressions.size()) {
      throw new IllegalArgumentException(String.format("Different sizes %d (values) vs %d (paths)", values.size(), expressions.size()));
    }

    final Map<Expression, Object> valuesAsMap = Maps.newHashMapWithExpectedSize(values.size());
    for (int i = 0; i < values.size(); i++) {
      valuesAsMap.put(expressions.get(i), values.get(i));
    }

    this.valuesAsMap = valuesAsMap;
    this.values = Collections.unmodifiableList(values);
    this.expressions = expressions;
  }

  public Object get(Expression expression) {
    Objects.requireNonNull(expression, "expression");
    Preconditions.checkArgument(valuesAsMap.containsKey(expression), "expression %s not present in tuple", expression);
    return valuesAsMap.get(expression);
  }

  public List<Expression> paths() {
    return expressions;
  }

  public List<?> values() {
    return this.values;
  }

  public static ProjectedTuple of(Iterable<Expression> paths, Iterable<?> values) {
    // values can be null
    return new ProjectedTuple(ImmutableList.copyOf(paths), Lists.newArrayList(values));
  }

  public static ProjectedTuple ofSingle(Expression path, Object value) {
    return of(ImmutableList.of(path), Collections.singleton(value));
  }

}
