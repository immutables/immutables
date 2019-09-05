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

package org.immutables.criteria.repository.async;

import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Query;

import java.util.Objects;

/**
 * Readable which uses {@link java.util.concurrent.CompletionStage}
 */
public class AsyncReadable<T> implements AsyncRepository.Readable<T> {

  private final Backend.Session session;

  public AsyncReadable(Backend.Session session) {
    this.session = Objects.requireNonNull(session, "session");
  }

  @Override
  public AsyncReader<T> find(Criterion<T> criteria) {
    return new AsyncReader<>(Criterias.toQuery(criteria), session);
  }

  @Override
  public AsyncReader<T> findAll() {
    return new AsyncReader<>(Query.of(session.entityType()), session);
  }
}
