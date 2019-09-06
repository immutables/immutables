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
import org.immutables.criteria.backend.DefaultResult;
import org.immutables.criteria.backend.IdExtractor;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Queries <a href="https://www.elastic.co/">ElasticSearch</a> data-store.
 */
public class ElasticsearchBackend implements Backend {

  final RestClient restClient;
  final ObjectMapper objectMapper;
  private final IndexResolver resolver;
  private final int scrollSize;

  public ElasticsearchBackend(ElasticsearchSetup setup) {
    Objects.requireNonNull(setup, "setup");
    this.restClient = setup.restClient();
    this.objectMapper = setup.objectMapper();
    this.resolver = setup.resolver();
    this.scrollSize = setup.scrollSize();
  }

  @Override
  public Backend.Session open(Class<?> entityType) {
    final String index = resolver.resolve(entityType);
    return new Session(entityType, new ElasticsearchOps(restClient, index, objectMapper, scrollSize));
  }

  @SuppressWarnings("unchecked")
  private static class Session implements Backend.Session {
    private final Class<?> entityType;
    private final ObjectMapper objectMapper;
    private final ElasticsearchOps ops;
    private final IdExtractor<Object, Object> idExtractor;
    private final JsonConverter<Object> converter;
    private final boolean hasId;


    private Session(Class<?> entityClass, ElasticsearchOps ops) {
      Objects.requireNonNull(entityClass, "entityClass");
      this.entityType = entityClass;
      this.ops = Objects.requireNonNull(ops, "ops");
      this.objectMapper = ops.mapper();
      IdExtractor<Object, Object> idExtractor = IdExtractor.from(x -> x);
      boolean hasId = false;
      try {
        idExtractor = IdExtractor.reflection((Class<Object>) entityClass);
        hasId = true;
      } catch (IllegalArgumentException ignore) {
        // id not supported
      }
      this.idExtractor = idExtractor;
      this.converter = DefaultConverter.<Object>of(objectMapper, entityClass);
      this.hasId = hasId;
    }

    @Override
    public Class<?> entityType() {
      return entityType;
    }

    @Override
    public Result execute(Operation query) {
      Objects.requireNonNull(query, "query");
      if (query instanceof StandardOperations.Insert) {
        return DefaultResult.of(insert((StandardOperations.Insert) query));
      } else if (query instanceof StandardOperations.Select) {
        return DefaultResult.of(select((StandardOperations.Select) query));
      }

      return DefaultResult.of(Flowable.error(new UnsupportedOperationException(String.format("Op %s not supported", query))));
    }

    private <T> Flowable<T> select(StandardOperations.Select op) {
      final Query query = op.query();
      if (query.hasAggregations()) {
        throw new UnsupportedOperationException("Aggregations not yet supported by " + ElasticsearchBackend.class.getSimpleName());
      }
      final ObjectNode json = query.filter().map(f -> Elasticsearch.converter(objectMapper).convert(f)).orElseGet(objectMapper::createObjectNode);
      query.limit().ifPresent(limit -> json.put("size", limit));
      query.offset().ifPresent(offset -> json.put("from", offset));
      if (!query.collations().isEmpty()) {
        final ArrayNode sort = json.withArray("sort");
        query.collations().forEach(c -> {
          sort.add(objectMapper.createObjectNode().put(c.path().toStringPath(), c.direction().isAscending() ? "asc" : "desc"));
        });
      }

      JsonConverter converter = this.converter;

      if (query.hasProjections()) {
        ArrayNode projection = query.projections().stream()
                 .map(p -> ((Path) p).toStringPath())
                 .reduce(objectMapper.createArrayNode(), ArrayNode::add, (old, newNode) -> newNode);
        json.set("_source", projection);
        converter = new ToTupleConverter(query, objectMapper);
      }

      final Flowable<T> flowable;
      if (query.offset().isPresent()) {
        // scroll doesn't work with offset
        flowable = ops.search(json, (JsonConverter<T>) converter);
      } else {
        flowable = ops.scrolledSearch(json, (JsonConverter<T>) converter);
      }

      return flowable;
    }

    private Publisher<WriteResult> insert(StandardOperations.Insert insert) {
      if (insert.values().isEmpty()) {
        return Flowable.just(WriteResult.empty());
      }

      // sets _id attribute (if entity has @Criteria.Id annotation)
      final BiFunction<Object, ObjectNode, ObjectNode> idFn = (entity, node) ->
              hasId ? (ObjectNode) node.set("_id", objectMapper.valueToTree(idExtractor.extract(entity))) : node;

      final List<ObjectNode> docs = insert.values().stream()
              .map(e -> idFn.apply(e, objectMapper.valueToTree(e)))
              .collect(Collectors.toList());

      return Flowable.fromCallable(() -> {
        ops.insertBulk(docs);
        return WriteResult.unknown();
      });
    }
  }
}
