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
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.WriteResult;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

class SyncUpdate implements Callable<WriteResult> {

  private final GeodeBackend.Session session;
  private final StandardOperations.Update operation;
  private final Region<Object, Object> region;

  SyncUpdate(GeodeBackend.Session session, StandardOperations.Update operation) {
    this.session = session;
    this.operation = operation;
    this.region = session.region;
  }

  @Override
  public WriteResult call() throws Exception {
    if (operation.values().isEmpty()) {
      return WriteResult.empty();
    }

    Map<Object, Object> toInsert = operation.values().stream().collect(Collectors.toMap(session.idExtractor::extract, x -> x));
    Region<Object, Object> region = this.region;

    // use putAll for upsert
    if (operation.upsert()) {
      return upsert(toInsert);
    }

    long inserted = 0;
    long updated = 0;
    for (Map.Entry<Object, Object> entry: toInsert.entrySet()) {
      // need to iterate because update semantics which translates into map.replace()
      boolean wasInserted = region.replace(entry.getKey(), entry.getValue()) == null;
      if (wasInserted) {
        inserted++;
      } else {
        updated++;
      }
    }

    return GeodeWriteResult.of().withInsertedCount(inserted).withUpdatedCount(updated);
  }

  private WriteResult upsert(Map<Object, Object> toUpsert) {
    if (toUpsert.size() == 1) {
      // for single value upsert one can return more meaningful WriteResult
      Map.Entry<Object, Object> entry = toUpsert.entrySet().iterator().next();
      Object result = region.put(entry.getKey(), entry.getValue());
      return result == null ? GeodeWriteResult.of().withInsertedCount(1) : GeodeWriteResult.of().withUpdatedCount(1);
    }

    // generic case upsert in bulk
    // we don't really know how many records were inserted / updated
    region.putAll(toUpsert);
    return WriteResult.unknown();
  }
}
