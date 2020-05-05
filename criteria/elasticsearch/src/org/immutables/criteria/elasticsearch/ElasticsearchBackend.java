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
import com.google.common.base.Preconditions;
import io.reactivex.Flowable;
import org.elasticsearch.client.RestClient;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.DefaultResult;
import org.immutables.criteria.backend.KeyExtractor;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Queries <a href="https://www.elastic.co/">ElasticSearch</a> data-store.
 */
public class ElasticsearchBackend implements Backend {

  final RestClient restClient;
  final ObjectMapper objectMapper;
  private final IndexResolver resolver;
  private final KeyExtractor.Factory keyExtractorFactory;
  private final int scrollSize;
  private final PathNaming pathNaming;

  public ElasticsearchBackend(ElasticsearchSetup setup) {
    Objects.requireNonNull(setup, "setup");
    this.restClient = setup.restClient();
    this.objectMapper = setup.objectMapper();
    this.resolver = setup.indexResolver();
    this.scrollSize = setup.scrollSize();
    this.keyExtractorFactory = setup.keyExtractorFactory();
    this.pathNaming = PathNaming.defaultNaming();
  }

  @Override
  public Backend.Session open(Class<?> entityType) {
    final String index = resolver.resolve(entityType);
    return new Session(entityType, keyExtractorFactory.create(entityType), new ElasticsearchOps(restClient, index, objectMapper, scrollSize), pathNaming);
  }

  static class Session implements Backend.Session {
    final Class<?> entityType;
    final ObjectMapper objectMapper;
    final ElasticsearchOps ops;
    final KeyExtractor keyExtractor;
    final JsonConverter<Object> converter;
    private final boolean hasId;
    final PathNaming pathNaming;
    final Predicate<Path> idPredicate;

    private Session(Class<?> entityClass, KeyExtractor keyExtractor, ElasticsearchOps ops, PathNaming pathNaming) {
      Objects.requireNonNull(entityClass, "entityClass");
      this.entityType = entityClass;
      this.ops = Objects.requireNonNull(ops, "ops");
      this.objectMapper = ops.mapper();
      this.keyExtractor = keyExtractor;
      this.converter = DefaultConverter.<Object>of(objectMapper, entityClass);
      KeyExtractor.KeyMetadata metadata = keyExtractor.metadata();
      this.hasId = metadata.isKeyDefined();
      this.pathNaming = pathNaming;
      this.idPredicate = Elasticsearch.idPredicate(keyExtractor.metadata());
    }

    @Override
    public Class<?> entityType() {
      return entityType;
    }

    @Override
    public Result execute(Operation operation) {
      Objects.requireNonNull(operation, "operation");
      if (operation instanceof StandardOperations.Insert) {
        return DefaultResult.of(insert((StandardOperations.Insert) operation));
      } else if (operation instanceof StandardOperations.Select) {
        return DefaultResult.of(select((StandardOperations.Select) operation));
      } else if (operation instanceof StandardOperations.GetByKey) {
        return DefaultResult.of(getByKey((StandardOperations.GetByKey) operation));
      } else if (operation instanceof StandardOperations.DeleteByKey) {
        return DefaultResult.of(deleteByKey((StandardOperations.DeleteByKey) operation));
      } else if (operation instanceof StandardOperations.Delete) {
        return DefaultResult.of(delete((StandardOperations.Delete) operation));
      }

      return DefaultResult.of(Flowable.error(new UnsupportedOperationException(String.format("Op %s not supported", operation))));
    }

    private Flowable<ProjectedTuple> aggregate(StandardOperations.Select op) {
      final Query query = op.query();
      Preconditions.checkArgument(query.hasAggregations(), "No Aggregations");
      AggregateQueryBuilder builder = new AggregateQueryBuilder(query, objectMapper, ops.mapping, pathNaming, idPredicate);
      return ops.searchRaw(builder.jsonQuery(), Collections.emptyMap())
              .map(builder::processResult)
              .toFlowable()
              .flatMapIterable(x -> x);
    }

    private Flowable<?> select(StandardOperations.Select op) {
      final Query query = op.query();

      if (query.distinct()) {
        return Flowable.error(new UnsupportedOperationException("DISTINCT not yet supported by " + ElasticsearchBackend.class.getSimpleName()));
      }

      if (query.count()) {
        return new CountCall(op, this).call().toFlowable();
      }

      if (query.hasAggregations()) {
        return aggregate(op);
      }
      final ObjectNode json = objectMapper.createObjectNode();

      query.filter().ifPresent(f -> json.set("query", Elasticsearch.constantScoreQuery(objectMapper, pathNaming, idPredicate).convert(f)));
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

      final Flowable<?> flowable;
      if (query.offset().isPresent()) {
        // scroll doesn't work with offset
        flowable = ops.search(json, (JsonConverter<?>) converter);
      } else {
        flowable = ops.scrolledSearch(json, (JsonConverter<?>) converter);
      }

      return flowable;
    }

    private Publisher<WriteResult> insert(StandardOperations.Insert insert) {
      if (insert.values().isEmpty()) {
        return Flowable.just(WriteResult.empty());
      }

      // sets _id attribute (if entity has @Criteria.Id annotation)
      final BiFunction<Object, ObjectNode, ObjectNode> idFn = (entity, node) ->
              hasId ? (ObjectNode) node.set("_id", objectMapper.valueToTree(keyExtractor.extract(entity))) : node;

      final List<ObjectNode> docs = insert.values().stream()
              .map(e -> idFn.apply(e, objectMapper.valueToTree(e)))
              .collect(Collectors.toList());

      return ops.insertBulk(docs).toFlowable();
    }

    private Flowable<?> getByKey(StandardOperations.GetByKey op) {
      ObjectNode json = objectMapper.createObjectNode();
      ObjectNode query = QueryBuilders.idsQuery(op.keys()).toJson(objectMapper);
      json.set("query", query);
      return ops.scrolledSearch(json, converter);
    }

    private Flowable<WriteResult> deleteByKey(StandardOperations.DeleteByKey op) {
      ObjectNode json = objectMapper.createObjectNode();
      ObjectNode query = QueryBuilders.idsQuery(op.keys()).toJson(objectMapper);
      json.set("query", query);
      return ops.deleteByQuery(json).toFlowable();
    }

    private Flowable<WriteResult> delete(StandardOperations.Delete op) {
      Query query = op.query();
      ObjectNode json = objectMapper.createObjectNode();
      query.filter().ifPresent(f -> json.set("query", Elasticsearch.toBuilder(f, pathNaming, idPredicate).toJson(objectMapper)));
      return ops.deleteByQuery(json).toFlowable();
    }
  }
}
