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

import java.util.Optional;

/**
 * Defines real-time data change received by {@link Watcher}.
 * Each insert / delete / update will generate a change event.
 *
 * @param <T> entity type
 */
public interface WatchEvent<T> {

  /**
   * Type of operation which caused this event to be created
   */
  enum Operation {
    INSERT,
    DELETE,
    REPLACE,
    UPDATE
  }

  // TODO define key for WatchEvent / Repository etc.
  Object key();

  Optional<T> newValue();

  Operation operation();

}
