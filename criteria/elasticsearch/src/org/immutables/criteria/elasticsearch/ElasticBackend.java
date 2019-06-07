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
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.expression.Expressional;
import org.immutables.criteria.internal.Backend;
import org.immutables.criteria.internal.Query;
import org.immutables.criteria.internal.Reactive;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Queries <a href="https://www.elastic.co/">ElasticSearch</a> data-store.
 * @param <T>
 */
public class ElasticBackend<T> implements Backend<Query, T> {

  private final RestClient restClient;
  private final ObjectMapper mapper;
  private final Class<T> type;
  private final String index;

  public ElasticBackend(RestClient restClient,
                        Class<T> type,
                        ObjectMapper mapper,
                        String index) {
    this.restClient = Objects.requireNonNull(restClient, "restClient");
    this.type = Objects.requireNonNull(type, "type");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.index = Objects.requireNonNull(index, "index");
  }

  @Override
  public Publisher<T> execute(Query query) {
    Objects.requireNonNull(query, "query");

    return queryInternal(Criterias.toExpressional(query.criteria));
  }

  private Publisher<T> queryInternal(Expressional expressional) {
    final Request request = new Request("POST", String.format("/%s/_search", index));

    final String json = Elasticsearch.converter(mapper).convert(expressional.expression());

    request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

    return Reactive.flatMapIterable(Reactive.map(new AsyncRestPublisher(restClient, request), converter()), x -> x);
  }

  private Function<Response, List<T>> converter() {
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
