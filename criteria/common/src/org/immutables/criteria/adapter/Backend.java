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

package org.immutables.criteria.adapter;

import org.reactivestreams.Publisher;

/**
 * Abstraction of a asynchronous service used by adapters and modeled as a single abstract method (SAM).
 */
public interface Backend {

  /**
   * Apply an operation on the back-end. {@code operation} in this context can mean query / update
   * / insert  / index etc.
   *
   * <p>Produced publisher can represent empty, single, multiple or unbounded event flow.
   *
   * @param operation operation to be performed on the back-end.
   * @return empty, single, multiple or unbounded event flow
   */
  <T> Publisher<T> execute(Operation operation);

  /**
   * Generic operation to be executed on the back-end. Typical operations include
   * query, update, delete etc.
   */
  interface Operation {

  }

}
