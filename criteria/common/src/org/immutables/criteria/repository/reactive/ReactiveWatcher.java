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

import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.ImmutableWatch;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.expression.Queryable;
import org.immutables.criteria.backend.WatchEvent;
import org.immutables.criteria.repository.Watcher;
import org.reactivestreams.Publisher;

import java.util.Objects;

public final class ReactiveWatcher<T> implements Watcher<T, Publisher<WatchEvent<T>>>, Queryable {

  private final Backend.Session session;
  private final Query query;

  public ReactiveWatcher(Criterion<?> criteria, Backend.Session session) {
    this.session = Objects.requireNonNull(session, "backend");
    this.query = Criterias.toQuery(Objects.requireNonNull(criteria, "criteria"));
  }

  @Override
  public Publisher<WatchEvent<T>> watch() {
    return session.execute(ImmutableWatch.of(query));
  }

  @Override
  public Query query() {
    return query;
  }
}
