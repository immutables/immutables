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

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.AbstractReader;
import org.immutables.criteria.repository.Publishers;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

public class AsyncReader<T> extends AbstractReader<AsyncReader<T>> {

  private final Backend.Session session;

  public AsyncReader(Query query, Backend.Session session) {
    super(query, session);
    this.session = Objects.requireNonNull(session, "session");
  }

  @Override
  protected AsyncReader<T> newReader(Query query) {
    return new AsyncReader<>(query, session);
  }

  public CompletionStage<List<T>> fetch() {
    return Publishers.toListFuture(fetchInternal());
  }
}
