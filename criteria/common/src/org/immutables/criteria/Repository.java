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
 * @param <T>
 */
public interface Repository<T> {

  /**
   * Allows to chain operations (like adding {@code offset} / {@code limit}) on some particular query.
   */
  interface Reader<T> {
    Reader<T> limit(long limit);

    Reader<T> offset(long offset);

    Publisher<T> fetch();
  }

  interface Readable<T> extends Repository<T> {

    Reader<T> find(DocumentCriteria<T> criteria);

    Reader<T> findAll();

  }

  /**
   * Marker for a successful operation
   */
  enum Success {
    SUCCESS
  }

  interface Writable<T> extends Repository<T> {

    default Publisher<Success> insert(T ... docs) {
      return insert(Arrays.asList(docs));
    }

    Publisher<Success> insert(Iterable<? extends T> docs);

    Publisher<Success> delete(DocumentCriteria<T> criteria);

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
