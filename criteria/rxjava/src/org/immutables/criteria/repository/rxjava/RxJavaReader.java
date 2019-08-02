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

package org.immutables.criteria.repository.rxjava;

import io.reactivex.Flowable;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.AbstractReader;

/**
 * Reader returning {@link Flowable} type
 */
public class RxJavaReader<T> extends AbstractReader<T, RxJavaReader<T>> {

  private final Backend backend;

  public RxJavaReader(Query query, Backend backend) {
    super(query, backend);
    this.backend = backend;
  }

  @Override
  protected RxJavaReader<T> newReader(Query query) {
    return new RxJavaReader<>(query, backend);
  }

  /**
   * Fetch available results in async fashion
   */
  public Flowable<T> fetch() {
    return Flowable.fromPublisher(fetchInternal());
  }
}
