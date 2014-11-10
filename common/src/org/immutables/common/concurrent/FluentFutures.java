/*
    Copyright 2013-2014 Immutables.org authors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.common.concurrent;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static com.google.common.base.Preconditions.*;

/**
 * Provides default wrapping implementation of {@link FluentFuture}
 */
public final class FluentFutures {
  private FluentFutures() {}

  private static class WrapingFluentFuture<V>
      extends SimpleForwardingListenableFuture<V>
      implements FluentFuture<V> {

    private final Executor executor;

    private WrapingFluentFuture(ListenableFuture<V> future, Executor executor) {
      super(future);
      this.executor = checkNotNull(executor);
    }

    @Override
    public V getUnchecked() {
      return Futures.getUnchecked(this);
    }

    @Override
    public FluentFuture<V> addCallback(FutureCallback<V> callback) {
      Futures.addCallback(this, callback, executor);
      return this;
    }

    @Override
    public FluentFuture<V> withFallback(FutureFallback<V> fallback) {
      return from(Futures.withFallback(this, fallback, executor));
    }

    @Override
    public FluentFuture<V> withFallbackValue(final V value) {
      return withFallback(new FutureFallback<V>() {
        @Override
        public ListenableFuture<V> create(Throwable t) throws Exception {
          return Futures.immediateFuture(value);
        }
      });
    }

    @Override
    public <T> FluentFuture<T> transform(Function<? super V, ? extends T> function) {
      return from(Futures.transform(this, function, executor));
    }

    @Override
    public <T> FluentFuture<T> transform(AsyncFunction<? super V, ? extends T> function) {
      return from(Futures.transform(this, function, executor));
    }

    @Override
    public FluentFuture<V> withExecutor(Executor executor) {
      if (this.executor == executor) {
        return this;
      }
      return new WrapingFluentFuture<V>(delegate(), executor);
    }

    @Override
    public <T> FluentFuture<T> lazyTransform(Function<? super V, ? extends T> function) {
      return new LazyTransformedFluentFuture<T, V>(this, function, executor);
    }
  }

  private static class LazyTransformedFluentFuture<V, F> extends WrapingFluentFuture<V> {
    private final Function<? super F, ? extends V> function;
    private final ListenableFuture<F> fromFuture;

    // Any get operation will go through transform function
    @SuppressWarnings("unchecked")
    LazyTransformedFluentFuture(
        ListenableFuture<F> fromFuture,
        Function<? super F, ? extends V> function,
        Executor executor) {
      super((ListenableFuture<V>) fromFuture, executor);
      this.fromFuture = fromFuture;
      this.function = function;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
      return function.apply(fromFuture.get());
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return function.apply(fromFuture.get(timeout, unit));
    }
  }

  /**
   * Unnecessary conversion, already have a fluent future. This method exists solely to detect
   * unnecessary conversion from a {@link FluentFuture}.
   * @deprecated you don't need to convert to a fluent future, it is already is
   * @param <V> value type
   * @param future future
   * @return same instance
   */
  @Deprecated
  public static <V> FluentFuture<V> from(FluentFuture<V> future) {
    return future;
  }

  /**
   * Wraps listenable future with a fluent future.
   * @param <V> value type
   * @param future future
   * @return fluent instance
   */
  public static <V> FluentFuture<V> from(ListenableFuture<V> future) {
    if (future instanceof FluentFuture<?>) {
      return (FluentFuture<V>) future;
    }
    return new WrapingFluentFuture<V>(future, MoreExecutors.directExecutor());
  }
}
