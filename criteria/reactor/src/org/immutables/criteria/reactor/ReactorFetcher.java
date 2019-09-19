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

package org.immutables.criteria.reactor;

import org.immutables.criteria.backend.NonUniqueResultException;
import org.immutables.criteria.repository.Fetcher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactorFetcher<T> extends Fetcher<T> {

  Flux<T> fetch();

  /**
   * Check that <i>exactly one</i> element is matched by current query and return it.
   * @return Mono with matched element or {@link NonUniqueResultException} (if result size is not one)
   */
  Mono<T> one();

  /**
   * Check that <i>at most one</i> element is matched by current query and return it (if available).
   * @return Mono as result or {@link NonUniqueResultException} (if result size is greater than one)
   */
  Mono<T> oneOrNone();

  /**
   * Check that current query matches any elements.
   * @return Mono with {@code true} if there are any matches / {@code false} otherwise
   */
  Mono<Boolean> exists();
}
