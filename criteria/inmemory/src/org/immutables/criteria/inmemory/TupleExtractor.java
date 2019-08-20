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

package org.immutables.criteria.inmemory;

import com.google.common.base.Preconditions;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;

import java.util.List;
import java.util.stream.Collectors;

class TupleExtractor  {

  private final Query query;

  TupleExtractor(Query query) {
    Preconditions.checkArgument(!query.projections().isEmpty(), "no projections defined");
    this.query = query;
  }

  ProjectedTuple extract(Object instance) {
    ReflectionFieldExtractor<?> extractor = new ReflectionFieldExtractor<>(instance);
    List<Object> values = query.projections().stream().map(e -> (Path) e)
            .map(extractor::extract).collect(Collectors.toList());
    return ProjectedTuple.of(query.projections(), values);
  }

}
