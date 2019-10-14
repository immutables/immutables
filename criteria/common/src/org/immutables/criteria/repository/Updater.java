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

package org.immutables.criteria.repository;

import org.immutables.criteria.matcher.Projection;

/**
 * Update DSL.
 *
 * Responsible for updates matching a filter (aka update by query). Attributes can be individually set or whole record
 * replaced.
 *
 * @param <T> type of the entity
 * @param <R> write result type usually sync/async wrapper of {@link org.immutables.criteria.backend.WriteResult}
 */
public interface Updater<T, R> {

  /**
   * Set particular attribute to a value
   */
  <T> Setter<R> set(Projection<T> projection, T value);

  /**
   * Replace matching element(s) with a new value
   */
  WriteExecutor<R> replace(T newValue);

  /**
   * Allows chaining setters on different attributes.
   *
   * <pre>
   * {@code
   *    repository.update(person.age.is(22))
   *              .set(person.name, "John")
   *              .set(person.age, 23)
   *              .execute();
   * }
   * </pre>
   * @param <R> write result type usually sync/async wrapper of {@link org.immutables.criteria.backend.WriteResult}
   */
  interface Setter<R> extends WriteExecutor<R> {

    /**
     * Set particular attribute to a value
     */
    <T> Setter<R> set(Projection<T> projection, T value);

  }

  interface OrInsert<T> {
    T orInsert();
  }

  /**
   * Part of repository DSL used as a <i>terminator</i> (last) command before returning
   * the result.
   *
   * @param <R>
   */
  interface WriteExecutor<R> {
    /**
     * Execute previously defined operation
     */
    R execute();
  }
}
