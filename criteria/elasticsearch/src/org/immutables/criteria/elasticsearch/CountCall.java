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

package org.immutables.criteria.elasticsearch;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.Single;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.expression.Query;

import java.util.concurrent.Callable;

/**
 * Responsible for {@code COUNT(*)} operation
 */
class CountCall implements Callable<Single<Long>> {

  private final StandardOperations.Select operation;
  private final ElasticsearchBackend.Session session;

  CountCall(StandardOperations.Select operation, ElasticsearchBackend.Session session) {
    this.operation = operation;
    this.session = session;
  }

  @Override
  public Single<Long> call() {
    Query query = operation.query();
    if (query.count() && (query.hasAggregations() || !query.groupBy().isEmpty())) {
      throw new UnsupportedOperationException("count(*) doesn't work with existing aggregates");
    }

    if (query.limit().isPresent() || query.offset().isPresent()) {
      throw new UnsupportedOperationException("count(*) doesn't work with limit / offset");
    }

    ObjectNode filter =  query.filter()
            .map(f -> Elasticsearch.toBuilder(f, session.pathNaming, session.idPredicate).toJson(session.objectMapper))
            .orElse(session.objectMapper.createObjectNode());

    if (filter.size() != 0) {
      filter = (ObjectNode) session.objectMapper.createObjectNode().set("query", filter);
    }
    return session.ops.count(filter).map(Json.Count::count);
  }
}
