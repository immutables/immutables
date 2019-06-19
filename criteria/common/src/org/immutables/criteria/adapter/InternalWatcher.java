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

package org.immutables.criteria.adapter;

import org.immutables.criteria.Criterias;
import org.immutables.criteria.DocumentCriteria;
import org.immutables.criteria.Repository;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

import java.util.Objects;

public final class InternalWatcher<T> implements Repository.Watcher<T> {

  private final Backend backend;
  private final Query query;

  public InternalWatcher(DocumentCriteria<?> criteria, Backend backend) {
    this.backend = Objects.requireNonNull(backend, "backend");
    this.query = Criterias.toQuery(Objects.requireNonNull(criteria, "criteria"));
  }

  @Override
  public Publisher<T> watch() {
    return backend.execute(ImmutableWatch.of(query));
  }

}
