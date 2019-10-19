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

import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.matcher.Projection;
import org.immutables.criteria.repository.Updater;
import org.immutables.criteria.repository.reactive.ReactiveUpdater;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Objects;

class ReactorUpdaterDelegate<T> implements Updater<T, Mono<WriteResult>> {

  private final ReactiveUpdater<T> delegate;

  ReactorUpdaterDelegate(ReactiveUpdater<T> delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public <T1> Setter<Mono<WriteResult>> set(Projection<T1> projection, T1 value) {
    return new ReactorSetter<>(delegate.set(projection, value));
  }

  @Override
  public WriteExecutor<Mono<WriteResult>> replace(T newValue) {
    return () -> Mono.from(delegate.replace(newValue).execute());
  }

  @SuppressWarnings("unchecked")
  private static class ReactorSetter<T> implements Updater.Setter<Mono<WriteResult>> {

    private final ReactiveUpdater.ReactiveSetter delegate;

    private ReactorSetter(ReactiveUpdater.ReactiveSetter delegate) {
      this.delegate = delegate;
    }

    @Override
    public <V> ReactorSetter<T> set(Projection<V> projection, V value) {
      return new ReactorSetter<>(delegate.set(projection, value));
    }

    @Override
    public Mono<WriteResult> execute() {
      Publisher<WriteResult> execute = delegate.execute();
      return Mono.from(execute);
    }
  }

}
