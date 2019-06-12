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

/**
 * Represents a constant scalar value.
 * @param <T> the value type
 */
final class FlowableJust<T> implements Publisher<T> {
  private final T value;

  FlowableJust(final T value) {
    this.value = Objects.requireNonNull(value, "value");
  }

  protected void subscribeActual(Subscriber<? super T> s) {
    s.onSubscribe(new ScalarSubscription<T>(s, value));
  }

  @Override
  public void subscribe(Subscriber<? super T> s) {
    subscribeActual(s);
  }
}
