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

package org.immutables.criteria.repository.sync;

import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Query;

import java.util.Objects;

/**
 * Synchronous (blocking) read operations on the repository.
 * @param <T> entity type
 */
public class SyncReadable<T> implements SyncRepository.Readable<T> {

  private final Query query;
  private final Backend backend;

  public SyncReadable(Class<T> entityClass, Backend backend) {
    Objects.requireNonNull(entityClass, "entityClass");
    this.query = Query.of(entityClass);
    this.backend = Objects.requireNonNull(backend, "backend");
  }

  @Override
  public SyncReader<T> find(Criterion<T> criteria) {
    return new SyncReader<>(Criterias.toQuery(criteria), backend);
  }

  @Override
  public SyncReader<T> findAll() {
    return new SyncReader<>(query, backend);
  }
}
