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

/**
 * Simple implementation of reader interface. Keeps immutable query internally.
 */
public final class InternalReader<T> implements Repository.Reader<T> {

  private final Query query;
  private final Backend backend;

  public InternalReader(DocumentCriteria<T> criteria, Backend backend) {
    this(Criterias.toQuery(criteria), backend);
  }

  private InternalReader(Query query, Backend backend) {
    this.query = Objects.requireNonNull(query, "query");
    this.backend = Objects.requireNonNull(backend, "backend");
  }

  @Override
  public Repository.Reader<T> limit(long limit) {
    return new InternalReader<T>(query.withLimit(limit), backend);
  }

  @Override
  public Repository.Reader<T> offset(long offset) {
    return new InternalReader<T>(query.withOffset(offset), backend);
  }

  @Override
  public Publisher<T> fetch() {
    return backend.execute(Operations.Select.of(query));
  }

}
