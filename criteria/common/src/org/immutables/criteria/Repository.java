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

package org.immutables.criteria;

import org.reactivestreams.Publisher;

/**
 * Access abstractions to a data-source.
 *
 * @param <T>
 */
public interface Repository<T> {

  /**
   * Allows to chain operations (like adding {@code offset} / {@code limit}) on some particular query.
   *
   * TODO: Think about Reader vs Finder which also has delete methods
   */
  interface Reader<T> {
    Publisher<T> fetch();
  }

  interface Readable<T> extends Repository<T> {

    Reader<T> find(DocumentCriteria<T> criteria);

    Reader<T> findAll();

  }

  interface Streamer<T> {
    Publisher<T> stream();
  }

  /**
   * Means current repository supports streaming (as in pub/sub).
   */
  interface Streamable<T> extends Repository<T> {
    Streamer<T> observe(DocumentCriteria<T> criteria);
  }

  // TODO think about Updater / Replacer interfaces

}
