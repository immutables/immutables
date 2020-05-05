/*
 * Copyright 2020 Immutables Authors and Contributors
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

package org.immutables.criteria.geode;

import com.google.common.collect.ImmutableSet;
import org.apache.geode.cache.Region;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.WriteResult;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Responsible for delete by key. Using bulk API
 *
 * @see Region#removeAll(Collection)
 */
class SyncDeleteByKey implements Callable<WriteResult> {

  private final Set<?> keys;
  private final GeodeBackend.Session session;

  SyncDeleteByKey(GeodeBackend.Session session, StandardOperations.DeleteByKey operation) {
    this(session, operation.keys());
  }

  SyncDeleteByKey(GeodeBackend.Session session, Iterable<?> keys) {
    this.keys = ImmutableSet.copyOf(keys);
    this.session = Objects.requireNonNull(session, "session");
  }

  @Override
  public WriteResult call() throws Exception {
    if (keys.isEmpty()) {
      return WriteResult.empty();
    }

    // special case for single key delete
    // Not using removeAll() because one can know if element was deleted based on return of remove()
    // this return is used for WriteResult statistics
    if (keys.size() == 1) {
      boolean removed = session.region.remove(keys.iterator().next()) != null;
      return WriteResult.empty().withDeletedCount(removed ? 1 :0);
    }
    session.region.removeAll(keys);
    return WriteResult.unknown();
  }
}
