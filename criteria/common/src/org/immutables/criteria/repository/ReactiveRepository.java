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

package org.immutables.criteria.repository;

import org.reactivestreams.Publisher;

/**
 * Repository based on <a href="https://www.reactive-streams.org/">Reactive Streams</a>.
 * All final operations will return {@link Publisher}.
 */
public class ReactiveRepository {

  public interface Readable<T> extends org.immutables.criteria.repository.Readable<T, ReactiveReader<T>> {

  }

  public interface Writable<T> extends org.immutables.criteria.repository.Writable<T, Publisher<WriteResult>> {

  }

  public interface Watchable<T> extends org.immutables.criteria.repository.Watchable<T, ReactiveWatcher<T>> {

  }

}
