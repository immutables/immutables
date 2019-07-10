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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.Flowable;
import org.elasticsearch.client.RestClient;
import org.immutables.criteria.WriteResult;
import org.immutables.criteria.adapter.Backend;
import org.immutables.criteria.adapter.Operations;
import org.immutables.criteria.adapter.UnknownWriteResult;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Queries <a href="https://www.elastic.co/">ElasticSearch</a> data-store.
 */
public class ElasticsearchBackend implements Backend {

  private final RestClient restClient;
  private final ObjectMapper mapper;
  private final String index;
  private final ElasticsearchOps ops;


  public ElasticsearchBackend(RestClient restClient,
                              ObjectMapper mapper,
                              String index) {
    this.restClient = Objects.requireNonNull(restClient, "restClient");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.index = Objects.requireNonNull(index, "index");
    this.ops = new ElasticsearchOps(restClient, index, mapper);
  }

  @Override
  public <T> Publisher<T> execute(Operation<T> query) {
    Objects.requireNonNull(query, "query");
    if (query instanceof Operations.KeyedInsert) {
      return (Publisher<T>) keyedInsert((Operations.KeyedInsert<Object, Object>) query);
    } else if (query instanceof Operations.Select) {
      return select((Operations.Select<T>) query);
    }

    return Flowable.error(new UnsupportedOperationException(String.format("Op %s not supported", query)));
  }

  private <T> Flowable<T> select(Operations.Select<T> op) {
    final Query query = op.query();
    final ObjectNode json = query.filter().map(f -> Elasticsearch.converter(mapper).convert(f)).orElseGet(mapper::createObjectNode);
    query.limit().ifPresent(limit -> json.put("size", limit));
    query.offset().ifPresent(offset -> json.put("from", offset));
    if (!query.collations().isEmpty()) {
      final ArrayNode sort = json.withArray("sort");
      query.collations().forEach(c -> {
        sort.add(mapper.createObjectNode().put(c.path().toStringPath(), c.direction().isAscending() ? "asc" : "desc"));
      });
    }

    return ops.search().apply(json)
            .map(r -> ops.responseConverter().apply(r).searchHits().hits())
            .toFlowable()
            .flatMapIterable(x -> x)
            .map(x -> ops.jsonConverter((Class<T>) query.entityPath().annotatedElement()).apply(x.source()));
  }

  private Publisher<WriteResult> keyedInsert(Operations.KeyedInsert<Object, Object> insert) {

    List<ObjectNode> docs = insert.entries().stream()
            .map(e -> (ObjectNode) ((ObjectNode) mapper.valueToTree(e.getValue())).set("_id", mapper.valueToTree(e.getKey())))
            .collect(Collectors.toList());

    return Flowable.fromCallable(() -> {
      ops.insertBulk(docs);
      return UnknownWriteResult.INSTANCE;
    });
  }

}
