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

import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.matcher.Projection;
import org.immutables.criteria.repository.Publishers;
import org.immutables.criteria.repository.Updater;
import org.immutables.criteria.repository.reactive.ReactiveUpdater;
import org.reactivestreams.Publisher;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

class AsyncUpdaterDelegate<T> implements Updater<T, CompletionStage<WriteResult>>  {

  private final ReactiveUpdater<T> delegate;

  AsyncUpdaterDelegate(ReactiveUpdater<T> delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public <T1> AsyncSetter set(Projection<T1> projection, T1 value) {
    return new AsyncSetter(delegate.set(projection, value));
  }

  @Override
  public WriteExecutor<CompletionStage<WriteResult>> replace(T newValue) {
    return new AsyncExecutor(delegate.replace(newValue));
  }


  private static class AsyncExecutor implements WriteExecutor<CompletionStage<WriteResult>> {
    private final WriteExecutor<Publisher<WriteResult>> delegate;

    private AsyncExecutor(WriteExecutor<Publisher<WriteResult>> delegate) {
      this.delegate = delegate;
    }

    @Override
    public CompletionStage<WriteResult> execute() {
      return Publishers.toFuture(delegate.execute());
    }
  }

  private static class AsyncSetter implements Updater.Setter<CompletionStage<WriteResult>> {

    private final ReactiveUpdater.ReactiveSetter delegate;

    private AsyncSetter(ReactiveUpdater.ReactiveSetter delegate) {
      this.delegate = delegate;
    }

    @Override
    public <T> AsyncSetter set(Projection<T> projection, T value) {
      return new AsyncSetter(delegate.set(projection, value));
    }

    @Override
    public CompletionStage<WriteResult> execute() {
      return Publishers.toFuture(delegate.execute());
    }
  }
}
