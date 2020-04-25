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
import org.immutables.criteria.expression.Expression;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Responsible for delete operations
 */
class SyncDelete implements Callable<WriteResult> {

  private final GeodeBackend.Session session;
  private final StandardOperations.Delete operation;
  private final Region<Object, Object> region;

  SyncDelete(GeodeBackend.Session session, StandardOperations.Delete operation) {
    this.session = session;
    this.operation = operation;
    this.region = session.region;
  }

  @Override
  public WriteResult call() throws Exception {
    if (!operation.query().filter().isPresent()) {
      // no filter means delete all (ie clear whole region)
      region.clear();
      return WriteResult.unknown();
    }

    Expression filter = operation.query().filter().orElseThrow(() -> new IllegalStateException("For " + operation));

    // special case when expression may contain only ID / key attribute
    // assume idProperty is defined (see IdResolver)
    KeyLookupAnalyzer.Result result = session.keyLookupAnalyzer.analyze(filter);
    if (result.isOptimizable()) {
      return deleteByKeys(result.values());
    }

    GeodeQueryVisitor visitor = new GeodeQueryVisitor(true, path -> String.format("e.value.%s", session.pathNaming.name(path)));
    Oql oql = filter.accept(visitor);

    // delete by query. Perform separate query to get list of IDs
    String query = String.format("select distinct e.key from %s.entries e where %s", region.getFullPath(), oql.oql());
    Collection<?> keys = (Collection<?>) session.queryService.newQuery(query).execute(oql.variables().toArray(new Object[0]));
    deleteByKeys(keys);
    return WriteResult.empty().withDeletedCount(keys.size());
  }

  /**
   * Used for special-case delete by key operation
   * @param keys list of keys to delete
   * @see Region#removeAll(Collection)
   */
  private WriteResult deleteByKeys(Collection<?> keys) {
    if (keys.isEmpty()) {
      return WriteResult.empty();
    }

    // special case for single key delete
    // Not using removeAll() because one can know if element was deleted based on return of remove()
    // this return is used for WriteResult statistics
    if (keys.size() == 1) {
      boolean removed = region.remove(keys.iterator().next()) != null;
      return WriteResult.empty().withDeletedCount(removed ? 1 :0);
    }

    region.removeAll(keys);
    return WriteResult.unknown(); // can't really return keys.size()
  }
}
