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

package org.immutables.criteria.repository.sync;

import org.immutables.criteria.backend.NonUniqueResultException;
import org.immutables.criteria.repository.Fetcher;

import java.util.List;
import java.util.Optional;

public interface SyncFetcher<T> extends Fetcher<T> {

  List<T> fetch();

  /**
   * Check that <i>exactly one</i> element is matched by current query and return it.
   * @throws NonUniqueResultException if result size is not one
   * @return matched element
   */
  T one();

  /**
   * Check that <i>at most one</i> element is matched by current query and return it (if available).
   * @return Publisher with zero or one element, or Publisher that throws {@link NonUniqueResultException}
   */
  Optional<T> oneOrNone();

  /**
   * Check that current query matches any elements.
   * @return {@code true} if there are any matches / {@code false} otherwise
   */
  boolean exists();


}
