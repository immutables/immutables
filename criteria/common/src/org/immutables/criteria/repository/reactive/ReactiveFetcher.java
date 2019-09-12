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

package org.immutables.criteria.repository.reactive;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.Fetcher;
import org.reactivestreams.Publisher;

import java.util.function.Function;

/**
 * Reactive interface for fetcher
 */
public interface ReactiveFetcher<T> extends Fetcher<T> {

  /**
   * Fetch all results
   */
  Publisher<T> fetch();

  /**
   * Check that <i>exactly one</i> element is matched by current query and return it.
   * @return Publisher with single element, or Publisher that throws {@link org.immutables.criteria.backend.NonUniqueResultException}
   */
  Publisher<T> one();

  /**
   * Check that <i>at most one</i> element is matched by current query and return it (if available).
   * @return Publisher with zero or one element, or Publisher that throws {@link org.immutables.criteria.backend.NonUniqueResultException}
   */
  Publisher<T> oneOrNone();

  /**
   * Check that current query matches any elements.
   * @return Publisher with single boolean ({@code true / false} if there is a match)
   */
  Publisher<Boolean> exists();

  /**
   * Applies a mapping function to each element emitted by current fetcher
   */
  <X> ReactiveFetcher<X> map(Function<? super T, ? extends X> mapFn);

  /**
   * Factory method
   */
  static <T> ReactiveFetcher<T> of(Query query, Backend.Session session) {
    return ReactiveFetcherDelegate.of(query, session);
  }

}
