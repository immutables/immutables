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

import com.google.common.collect.ImmutableList;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Path;

import java.util.List;

/**
 * Result of a projection
 */
public class ProjectedTuple {

  private final List<Expression> paths;
  private final List<?> values;

  private ProjectedTuple(List<Expression> paths, List<?> values) {
    if (values.size() != paths.size()) {
      throw new IllegalArgumentException(String.format("Different sizes %d (values) vs %d (paths)", values.size(), paths.size()));
    }
    this.values = values;
    this.paths = paths;
  }

  public List<Expression> paths() {
    return paths;
  }

  public List<?> values() {
    return this.values;
  }

  public static ProjectedTuple of(Iterable<Expression> paths, Iterable<?> values) {
    return new ProjectedTuple(ImmutableList.copyOf(paths), ImmutableList.copyOf(values));
  }

  public static ProjectedTuple ofSingle(Expression path, Object value) {
    return of(ImmutableList.of(path), ImmutableList.of(value));
  }
}
