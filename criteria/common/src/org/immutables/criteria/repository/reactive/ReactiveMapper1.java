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

package org.immutables.criteria.repository.reactive;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.Mappers;
import org.immutables.criteria.repository.Publishers;
import org.reactivestreams.Publisher;

class ReactiveMapper1<T1> {

  private final Query query;
  private final Backend.Session session;

  ReactiveMapper1(Query query, Backend.Session session) {
    this.query = query;
    this.session = session;
  }

  public <R> ReactiveFetcher<R> map() {
    return new ReactiveFetcher<ProjectedTuple>(query, session).map(Mappers.fromTuple());
  }
}
