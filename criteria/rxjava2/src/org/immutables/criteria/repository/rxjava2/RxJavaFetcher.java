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

package org.immutables.criteria.repository.rxjava2;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.immutables.criteria.backend.NonUniqueResultException;
import org.immutables.criteria.repository.Fetcher;

public interface RxJavaFetcher<T> extends Fetcher<T> {

  Flowable<T> fetch();

  /**
   * Check that <i>exactly one</i> element is matched by current query and return it.
   * @return Single with matched element or {@link NonUniqueResultException} if result size is not one
   */
  Single<T> one();

  /**
   * Check that <i>at most one</i> element is matched by current query and return it (if available).
   * @return Maybe as result or {@link NonUniqueResultException}
   */
  Maybe<T> oneOrNone();

  /**
   * Check that current query matches any elements.
   * @return Single with {@code true} if there are any matches / {@code false} otherwise
   */
  Single<Boolean> exists();

  /**
   * Count number of elements to be returned. Similar to {@code COUNT(*)} in SQL.
   * @return Single with number of elements
   */
  Single<Long> count();

  interface DistinctLimitOffset<T> extends LimitOffset<T> {
    LimitOffset<T> distinct();
  }

  interface LimitOffset<T> extends Offset<T> {
    Offset<T> limit(long limit);
  }

  interface Offset<T> extends RxJavaFetcher<T> {
    RxJavaFetcher<T> offset(long offset);
  }

}
