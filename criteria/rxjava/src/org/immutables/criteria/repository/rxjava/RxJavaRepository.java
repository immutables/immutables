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

import io.reactivex.Single;
import org.immutables.criteria.backend.WriteResult;

/**
 * Repository with <a href="https://github.com/ReactiveX/RxJava">rxjava</a> return types.
 */
public interface RxJavaRepository<T> {

  interface Readable<T> extends RxJavaRepository<T>, org.immutables.criteria.repository.Readable<T, RxJavaReader<T>> { }

  interface Writable<T> extends RxJavaRepository<T>, org.immutables.criteria.repository.Writable<T, Single<WriteResult>> { }

  interface Watchable<T> extends RxJavaRepository<T>, org.immutables.criteria.repository.Watchable<T, RxJavaWatcher<T>> { }

}
