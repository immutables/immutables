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

import org.immutables.criteria.repository.Repository;
import org.immutables.criteria.backend.WriteResult;

import java.util.List;

/**
 * Synchronous (blocking operation) repository.
 * Final operations return {@link List}, {@link Void} or other non-deferred results.
 *
 * @param <T> entity type
 */
public interface SyncRepository<T> extends Repository<T> {

  interface Readable<T> extends SyncRepository<T>, org.immutables.criteria.repository.Readable<T, SyncReader<T>> { }

  interface Writable<T> extends SyncRepository<T>, org.immutables.criteria.repository.Writable<T, WriteResult> { }


}
