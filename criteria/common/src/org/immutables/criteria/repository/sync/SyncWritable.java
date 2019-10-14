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

import org.immutables.criteria.Criterion;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.repository.Publishers;
import org.immutables.criteria.repository.Updater;
import org.immutables.criteria.repository.reactive.ReactiveWritable;

import java.util.Objects;

/**
 * Blocking write operations on the repository
 */
public class SyncWritable<T> implements SyncRepository.Writable<T> {

  private final ReactiveWritable<T> writable;

  public SyncWritable(Backend.Session session) {
    Objects.requireNonNull(session, "backend");
    this.writable = new ReactiveWritable<>(session);
  }

  @Override
  public WriteResult insertAll(Iterable<? extends T> docs) {
    return Publishers.blockingGet(writable.insertAll(docs));
  }

  @Override
  public WriteResult delete(Criterion<T> criteria) {
    return Publishers.blockingGet(writable.delete(criteria));
  }

  @Override
  public WriteResult upsertAll(Iterable<? extends T> docs) {
    return Publishers.blockingGet(writable.upsertAll(docs));
  }

  @Override
  public WriteResult updateAll(Iterable<? extends T> docs) {
    return Publishers.blockingGet(writable.updateAll(docs));
  }

  @Override
  public Updater<T, WriteResult> update(Criterion<T> criterion) {
    return new SyncUpdaterDelegate<>(writable.update(criterion));
  }

}
