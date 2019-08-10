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
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.Backends;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Queries <a href="https://www.elastic.co/">ElasticSearch</a> data-store.
 */
public class ElasticsearchBackend implements Backend {

  final RestClient restClient;
  final ObjectMapper mapper;
  private final IndexResolver resolver;
  private final int scrollSize;

  public ElasticsearchBackend(RestClient restClient,
                              ObjectMapper mapper,
                              IndexResolver resolver) {
    this(restClient, mapper, resolver, 1024);
  }

  ElasticsearchBackend(RestClient restClient,
                              ObjectMapper mapper,
                              IndexResolver resolver,
                              int scrollSize) {
    this.restClient = Objects.requireNonNull(restClient, "restClient");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.resolver = Objects.requireNonNull(resolver, "resolver");
    this.scrollSize = scrollSize;
  }


  @Override
  public Backend.Session open(Class<?> entityType) {
    final String index = resolver.resolve(entityType);
    return new Session(entityType, new ElasticsearchOps(restClient, index, mapper, scrollSize));
  }

  private static class Session implements Backend.Session {
    private final ObjectMapper mapper;
    private final ElasticsearchOps ops;
    private final Function<Object, Object> idExtractor;
    private final boolean hasId;

    private Session(Class<?> entityClass, ElasticsearchOps ops) {
      Objects.requireNonNull(entityClass, "entityClass");
      this.ops = Objects.requireNonNull(ops, "ops");
      this.mapper = ops.mapper();
      Function<Object, Object> idExtractor = Function.identity();
      boolean hasId = false;
      try {
        idExtractor = Backends.idExtractor((Class<Object>) entityClass);
        hasId = true;
      } catch (IllegalArgumentException ignore) {
        // id not supported
      }
      this.idExtractor = idExtractor;
      this.hasId = hasId;
    }

    @Override
    public <T> Publisher<T> execute(Operation query) {
      Objects.requireNonNull(query, "query");
      if (query instanceof StandardOperations.Insert) {
        return (Publisher<T>) insert((StandardOperations.Insert<Object>) query);
      } else if (query instanceof StandardOperations.Select) {
        return select((StandardOperations.Select<T>) query);
      }

      return Flowable.error(new UnsupportedOperationException(String.format("Op %s not supported", query)));
    }

    private <T> Flowable<T> select(StandardOperations.Select<T> op) {
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

      final Class<T> type = (Class<T>) query.entityPath().annotatedElement();

      final Flowable<T> flowable;
      if (query.offset().isPresent()) {
        // scroll doesn't work with offset
        flowable = ops.search(json, type);
      } else {
        flowable = ops.scrolledSearch(json, type);
      }

      return flowable;
    }

    private Publisher<WriteResult> insert(StandardOperations.Insert<Object> insert) {
      if (insert.values().isEmpty()) {
        return Flowable.just(WriteResult.UNKNOWN);
      }

      // sets _id attribute (if entity has @Criteria.Id annotation)
      final BiFunction<Object, ObjectNode, ObjectNode> idFn = (entity, node) ->
              hasId ? (ObjectNode) node.set("_id", mapper.valueToTree(idExtractor.apply(entity))) : node;

      final List<ObjectNode> docs = insert.values().stream()
              .map(e -> idFn.apply(e, mapper.valueToTree(e)))
              .collect(Collectors.toList());

      return Flowable.fromCallable(() -> {
        ops.insertBulk(docs);
        return WriteResult.UNKNOWN;
      });
    }
  }
}
