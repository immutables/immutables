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
 * Access abstractions to a data-source. Read /  Write / Watch features can be added
 * declaratively by implementing special interfaces (eg {@link Readable}).
 *
 * @param <T> entity type
 */
public interface Repository<T> {

  /**
   * Allows chaining operations (like adding {@code offset} / {@code limit}) on some particular query.
   * Subclasses may add other attributes specific to their repository.
   *
   * @param <T> entity type
   * @param <R> reader type (self type)
   */
  interface Reader<T, R> {
    R limit(long limit);

    R offset(long offset);
  }

  /**
   * Means repository can perform find operations (similar to SQL {@code SELECT} statement)
   * @param <T> entity type
   * @param <R> self-type of reader
   */
  interface Readable<T, R extends Reader<T, R>> extends Repository<T> {

    R find(Criterion<T> criteria);

    R findAll();

  }

  /**
   * Declares repository as writable. Means objects can be inserted / updated / deleted.
   *
   * @param <T> entity type
   * @param <R> self return type
   */
  interface Writable<T, R> extends Repository<T> {

    default R insert(T ... docs) {
      return insert(Arrays.asList(docs));
    }

    R insert(Iterable<? extends T> docs);

    R delete(Criterion<T> criteria);

  }

  /**
   * Observer for, potentially, unbounded flow of events
   * @param <T> entity type
   */
  interface Watcher<T> {
    Publisher<T> watch();
  }

  /**
   * Means current repository supports streaming (as in pub/sub). In this case
   * the flow of events is unbounded.
   */
  interface Watchable<T> extends Repository<T> {
    Watcher<T> watcher(Criterion<T> criteria);
  }

}
