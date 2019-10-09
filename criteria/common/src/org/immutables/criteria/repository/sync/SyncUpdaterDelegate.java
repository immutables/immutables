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

import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.matcher.Projection;
import org.immutables.criteria.repository.Publishers;
import org.immutables.criteria.repository.Updater;
import org.immutables.criteria.repository.reactive.ReactiveUpdater;

import java.util.Objects;

class SyncUpdaterDelegate<T> implements Updater<T, WriteResult> {

  private final ReactiveUpdater<T> delegate;

  SyncUpdaterDelegate(ReactiveUpdater<T> delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public <T1> SyncSetter set(Projection<T1> projection, T1 value) {
    return new SyncSetter(delegate.set(projection, value));
  }

  @Override
  public Executor<WriteResult> replace(T newValue) {
    return () -> Publishers.blockingGet(delegate.replace(newValue).execute());
  }

  private static class SyncSetter implements Updater.Setter<WriteResult> {

    private final ReactiveUpdater.ReactiveSetter delegate;

    private SyncSetter(ReactiveUpdater.ReactiveSetter delegate) {
      this.delegate = delegate;
    }

    @Override
    public <T> SyncSetter set(Projection<T> projection, T value) {
      return new SyncSetter(delegate.set(projection, value));
    }

    @Override
    public WriteResult execute() {
      return Publishers.<WriteResult>blockingGet(delegate.execute());
    }
  }
}
