/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package org.immutables.criteria.internal.reactive;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Objects;
import java.util.concurrent.Callable;

final class FlowableError<T> implements Publisher<T> {

  final Callable<? extends Throwable> errorSupplier;

  FlowableError(Callable<? extends Throwable> errorSupplier) {
    this.errorSupplier = errorSupplier;
  }

  @Override
  public void subscribe(Subscriber<? super T> s) {
    Throwable error;
    try {
      error = Objects.requireNonNull(errorSupplier.call(), "Callable returned null throwable. Null values are generally not allowed in 2.x operators and sources.");
    } catch (Throwable t) {
      error = t;
    }
    EmptySubscription.error(error, s);
  }
}
