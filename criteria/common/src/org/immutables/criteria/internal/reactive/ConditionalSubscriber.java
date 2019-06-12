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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A Subscriber with an additional {@link #tryOnNext(Object)} method that
 * tells the caller the specified value has been accepted or
 * not.
 *
 * <p>This allows certain queue-drain or source-drain operators
 * to avoid requesting 1 on behalf of a dropped value.
 *
 * @param <T> the value type
 */
interface ConditionalSubscriber<T> extends Subscriber<T> {

  @Override
  void onSubscribe(Subscription s);

  /**
   * Conditionally takes the value.
   * @param t the value to deliver
   * @return true if the value has been accepted, false if the value has been rejected
   * and the next value can be sent immediately
   */
  boolean tryOnNext(T t);
}
