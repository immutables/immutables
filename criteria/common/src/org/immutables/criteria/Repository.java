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

import java.util.Arrays;

/**
 * Access abstractions to a data-source.
 *
 * @param <T> entity type
 */
public interface Repository<T> {

  /**
   * Allows to chain operations (like adding {@code offset} / {@code limit}) on some particular query.
   */
  interface Reader<T> {
    Reader<T> limit(long limit);

    Reader<T> offset(long offset);
  }

  interface Readable<T, R extends Reader<T>> extends Repository<T> {

    R find(DocumentCriteria<T> criteria);

    R findAll();

  }

  /**
   * Marker for a successful operation
   */
  enum Success {
    SUCCESS
  }

  interface Writable<T, R> extends Repository<T> {

    default R insert(T ... docs) {
      return insert(Arrays.asList(docs));
    }

    R insert(Iterable<? extends T> docs);

    R delete(DocumentCriteria<T> criteria);

  }

  interface Watcher<T> {
    Publisher<T> watch();
  }

  /**
   * Means current repository supports streaming (as in pub/sub).
   */
  interface Watchable<T> extends Repository<T> {
    Watcher<T> watcher(DocumentCriteria<T> criteria);
  }


}
