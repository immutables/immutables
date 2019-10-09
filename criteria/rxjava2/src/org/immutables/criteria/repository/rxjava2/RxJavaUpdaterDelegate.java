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

package org.immutables.criteria.repository.rxjava2;

import io.reactivex.Flowable;
import io.reactivex.Single;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.matcher.Projection;
import org.immutables.criteria.repository.Updater;
import org.immutables.criteria.repository.reactive.ReactiveUpdater;
import org.reactivestreams.Publisher;

import java.util.Objects;

class RxJavaUpdaterDelegate<T> implements Updater<T, Single<WriteResult >> {

  private final ReactiveUpdater<T> delegate;

  RxJavaUpdaterDelegate(ReactiveUpdater<T> delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public <V> Setter<Single<WriteResult>> set(Projection<V> projection, V value) {
    return new RxJavaSetter<>(delegate.set(projection, value));
  }

  @Override
  public Executor<Single<WriteResult>> replace(T newValue) {
    return () -> Flowable.fromPublisher(delegate.replace(newValue).execute()).singleOrError();
  }

  private static class RxJavaSetter<T> implements Updater.Setter<Single<WriteResult>> {

    private final ReactiveUpdater.ReactiveSetter delegate;

    private RxJavaSetter(ReactiveUpdater.ReactiveSetter delegate) {
      this.delegate = delegate;
    }

    @Override
    public <V> RxJavaSetter<T> set(Projection<V> projection, V value) {
      return new RxJavaSetter<>(delegate.set(projection, value));
    }

    @Override
    public Single<WriteResult> execute() {
      @SuppressWarnings("unchecked")
      Publisher<WriteResult> execute = delegate.execute();
      return Flowable.fromPublisher(execute).singleOrError();
    }
  }
}
