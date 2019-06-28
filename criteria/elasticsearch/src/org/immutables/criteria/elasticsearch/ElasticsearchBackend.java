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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.immutables.criteria.WriteResult;
import org.immutables.criteria.adapter.Backend;
import org.immutables.criteria.adapter.Operations;
import org.immutables.criteria.adapter.UnknownWriteResult;
import org.immutables.criteria.expression.Query;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
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
    final Request request = new Request("POST", String.format("/%s/_search", index));
    final Query query = op.query();
    final ObjectNode json = query.filter().map(f -> Elasticsearch.converter(mapper).convert(f)).orElseGet(mapper::createObjectNode);
    query.limit().ifPresent(limit -> json.put("size", limit));
    query.offset().ifPresent(offset -> json.put("from", offset));
    request.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

    return httpRequest(request)
            .map(r -> converter((Class<T>) query.entityPath().annotatedElement()).apply(r))
            .toFlowable()
            .flatMapIterable(x -> x);
  }

  /**
   * Perform async http request.
   * TODO scrolling API
   */
  private Single<Response> httpRequest(Request request) {
    return Single.create(source -> {
      restClient.performRequestAsync(request, new ResponseListener() {
        @Override
        public void onSuccess(Response response) {
          source.onSuccess(response);
        }

        @Override
        public void onFailure(Exception exception) {
          source.onError(exception);
        }
      });
    });
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

  private <T> Function<Response, List<T>> converter(Class<T> type) {
    return response -> {
      try (InputStream is = response.getEntity().getContent()) {
        final ObjectNode root = mapper.readValue(is, ObjectNode.class);
        final JsonNode hits = root.path("hits").path("hits");
        if (hits.isMissingNode()) {
          return Collections.emptyList();
        }

        final ArrayNode array = (ArrayNode) hits;
        final List<T> result = new ArrayList<>();
        for (JsonNode node: array) {
          result.add(mapper.treeToValue(node.get("_source"), type));
        }

        return result;
      } catch (IOException e) {
        final String message = String.format("Couldn't parse HTTP response %s into %s", response, type);
        throw new UncheckedIOException(message, e);
      }
    };
  }

}
