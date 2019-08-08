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

import org.immutables.criteria.Criterion;
import org.immutables.criteria.backend.Backend;

import java.util.Objects;

public class ReactiveWatchable<T> implements ReactiveRepository.Watchable<T> {

  private final Backend.Session session;

  public ReactiveWatchable(Backend.Session backend) {
    this.session = Objects.requireNonNull(backend, "session");
  }

  @Override
  public ReactiveWatcher<T> watcher(Criterion<T> criteria) {
    return new ReactiveWatcher<>(criteria, session);
  }

}
