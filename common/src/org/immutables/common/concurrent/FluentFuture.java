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
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.concurrent.CancellationException;

/**
 * {@link ListenableFuture} that is enhanced with ability to invoke several operations from
 * {@link Futures} as instance methods rather than static methods.
 * @param <V> The result type returned by this Future's <tt>get</tt> method
 */
public interface FluentFuture<V> extends ListenableFuture<V> {

  /**
   * Get value or throw unchecked exception. This method blocks till value is computed or exception
   * is thrown.
   * @see Futures#getUnchecked(java.util.concurrent.Future)
   * @return result value
   * @throws UncheckedExecutionException if {@code get} throws an {@code ExecutionException} with an
   *           {@code Exception} as its cause
   * @throws ExecutionError if {@code get} throws an {@code ExecutionException} with an
   *           {@code Error} as its cause
   * @throws CancellationException if {@code get} throws a {@code CancellationException}
   */
  V getUnchecked();

  /**
   * Add callback
   * @see Futures#addCallback(ListenableFuture, FutureCallback)
   * @param future The future attach the callback to.
   * @param callback The callback to invoke when {@code future} is completed.
   * @return {@code this} future
   */
  FluentFuture<V> addCallback(FutureCallback<V> callback);

  /**
   * With fallback.
   * @see Futures#withFallback(ListenableFuture, FutureFallback)
   * @param fallback the fallback
   * @return the fluent future wi
   */
  FluentFuture<V> withFallback(FutureFallback<V> fallback);

  /**
   * With fallback value.
   * @see Futures#withFallback(ListenableFuture, FutureFallback)
   * @see Futures#immediateFuture(Object)
   * @param fallback the {@link FutureFallback} implementation to be called if {@code input} fails
   * @param value the value
   * @return the fluent future
   */
  FluentFuture<V> withFallbackValue(V value);

  /**
   * Transform future using suppied function.
   * @see Futures#transform(ListenableFuture, Function)
   * @param <T> transformed type
   * @param function A Function to transform the results of the provided future
   *          to the results of the returned future. This will be run in the thread
   *          that notifies input it is complete.
   * @return A future that holds result of the transformation.
   */
  <T> FluentFuture<T> transform(Function<? super V, ? extends T> function);

  /**
   * Asyncronous transform using supplied async function.
   * @see Futures#transform(ListenableFuture, AsyncFunction)
   * @param <T> transformed type
   * @param function A function to transform the result of the input future
   *          to the result of the output future
   * @return A future that holds result of the function (if the input succeeded)
   *         or the original input's failure (if not)
   * @see Futures#transform(ListenableFuture, AsyncFunction)
   */
  <T> FluentFuture<T> transform(AsyncFunction<? super V, ? extends T> function);
}
