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

package org.immutables.criteria.repository.async;

import org.immutables.criteria.backend.NonUniqueResultException;
import org.immutables.criteria.repository.Fetcher;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface AsyncFetcher<T> extends Fetcher<T> {

  CompletionStage<List<T>> fetch();

  /**
   * Check that <i>exactly one</i> element is matched by current query and return it.
   * @throws NonUniqueResultException if result size is not one
   * @return matched element
   */
  CompletionStage<T> one();

  /**
   * Check that <i>at most one</i> element is matched by current query and return it (if available).
   * @return Future with optional element
   */
  CompletionStage<Optional<T>> oneOrNone();

  /**
   * Check that current query matches any elements.
   * @return Future holding  {@code true} value if there are any matches / {@code false} otherwise
   */
  CompletionStage<Boolean> exists();

  /**
   * Count number of elements to be returned. Similar to {@code COUNT(*)} in SQL.
   * @return Future holding number of elements
   */
  CompletionStage<Long> count();

}
