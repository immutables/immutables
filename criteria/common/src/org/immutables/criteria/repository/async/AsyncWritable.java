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

import org.immutables.criteria.Criterion;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.repository.Publishers;
import org.immutables.criteria.repository.Updater;
import org.immutables.criteria.repository.reactive.ReactiveWritable;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * Writable which uses {@link CompletionStage} as return type
 */
public class AsyncWritable<T> implements AsyncRepository.Writable<T> {

  private final ReactiveWritable<T> reactive;

  public AsyncWritable(Backend.Session session) {
    Objects.requireNonNull(session, "session");
    this.reactive = new ReactiveWritable<>(session);
  }

  @Override
  public CompletionStage<WriteResult> insertAll(Iterable<? extends T> docs) {
    return Publishers.toFuture(reactive.insertAll(docs));
  }

  @Override
  public CompletionStage<WriteResult> delete(Criterion<T> criteria) {
    return Publishers.toFuture(reactive.delete(criteria));
  }

  @Override
  public Updater<T, CompletionStage<WriteResult>> update(Criterion<T> criterion) {
    return new AsyncUpdaterDelegate<>(reactive.update(criterion));
  }

}
