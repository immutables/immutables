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

package org.immutables.criteria.geode;

import org.apache.geode.cache.Region;
import org.immutables.criteria.backend.BackendException;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.WriteResult;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

class SyncInsert implements Callable<WriteResult> {

  private final GeodeBackend.Session session;
  private final StandardOperations.Insert operation;
  private final Region<Object, Object> region;

  SyncInsert(GeodeBackend.Session session, StandardOperations.Insert operation) {
    this.session = session;
    this.operation = operation;
    this.region = session.region;
  }

  @Override
  public WriteResult call() throws Exception {
    if (operation.values().isEmpty()) {
      return WriteResult.empty();
    }

    final Map<Object, Object> toInsert = operation.values().stream().collect(Collectors.toMap(session.idExtractor::extract, x -> x));
    final Region<Object, Object> region = this.region;
    for (Map.Entry<Object, Object> entry: toInsert.entrySet()) {
      Object previous = region.putIfAbsent(entry.getKey(), entry.getValue());
      if (previous != null) {
        throw new BackendException(String.format("Duplicate id %s for %s", entry.getKey(), session.entityType()));
      }
    }

    return GeodeWriteResult.of().withInsertedCount(toInsert.size());
  }
}
