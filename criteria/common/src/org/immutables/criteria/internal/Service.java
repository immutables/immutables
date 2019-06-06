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

package org.immutables.criteria.internal;

import org.reactivestreams.Publisher;

/**
 * Abstraction of a asynchronous service used by adapters modeled as a function.
 *
 * @param <Q> type of the query. Can be string or something more complicated
 * @param <T> type of the result (computation)
 */
public interface Service<Q, T> {

  /**
   * Fetch (possibly unbounded) result of a computation.
   *
   * @param query payload
   * @return empty or unbounded flow of events
   */
  Publisher<T> execute(Q query);

}
