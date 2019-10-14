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

package org.immutables.criteria.reactor;

import org.immutables.criteria.Criterion;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.repository.Updater;
import org.immutables.criteria.repository.reactive.ReactiveWritable;
import reactor.core.publisher.Mono;

public class ReactorWritable<T> implements ReactorRepository.Writable<T> {

  private final ReactiveWritable<T> writable;

  public ReactorWritable(Backend.Session session) {
    this.writable = new ReactiveWritable<>(session);
  }

  @Override
  public Mono<WriteResult> insertAll(Iterable<? extends T> docs) {
    return Mono.from(writable.insertAll(docs));
  }

  @Override
  public Mono<WriteResult> delete(Criterion<T> criteria) {
    return Mono.from(writable.delete(criteria));
  }

  @Override
  public Updater<T, Mono<WriteResult>> update(Criterion<T> criterion) {
    return new ReactorUpdaterDelegate<>(writable.update(criterion));
  }
}
