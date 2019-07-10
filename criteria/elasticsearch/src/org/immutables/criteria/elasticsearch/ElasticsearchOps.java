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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import io.reactivex.Completable;
import io.reactivex.Single;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Helper methods to <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-put-mapping.html">define an index</a>
 * or insert documents in elastic search.
 */
class ElasticsearchOps {

  private final RestClient restClient;
  private final ObjectMapper mapper;
  private final String index;

  final int fetchSize;

  ElasticsearchOps(RestClient restClient, String index, ObjectMapper mapper) {
    this(restClient, index, mapper, 1024);
  }

  ElasticsearchOps(RestClient restClient, String index, ObjectMapper mapper, int fetchSize) {
    this.restClient = Objects.requireNonNull(restClient, "restClient");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.index = Objects.requireNonNull(index, "index");
    Preconditions.checkArgument(fetchSize > 0, "Negative fetchsize %s", fetchSize);
    this.fetchSize = fetchSize;
  }

  /**
   * Creates index in elastic search given a mapping. Mapping can contain nested fields expressed
   * as dots({@code .}).
   *
   * <p>Example
   * <pre>
   *  {@code
   *     b.a: long
   *     b.b: keyword
   *  }
   * </pre>
   *
   * @param mapping field and field type mapping
   * @throws IOException if there is an error
   */
  void createIndex(Map<String, String> mapping) throws IOException {
    Objects.requireNonNull(index, "index");
    Objects.requireNonNull(mapping, "mapping");

    ObjectNode mappings = mapper().createObjectNode();

    ObjectNode properties = mappings.with("mappings").with("properties");
    for (Map.Entry<String, String> entry: mapping.entrySet()) {
      applyMapping(properties, entry.getKey(), entry.getValue());
    }

    // create index and mapping
    final HttpEntity entity = new StringEntity(mapper().writeValueAsString(mappings),
            ContentType.APPLICATION_JSON);
    final Request r = new Request("PUT", "/" + index);
    r.setEntity(entity);
    restClient().performRequest(r);
  }

  void deleteIndex() throws IOException {
    final Request r = new Request("DELETE", "/" + index);
    restClient().performRequest(r);
  }

  /**
   * Creates nested mappings for an index. This function is called recursively for each level.
   *
   * @param parent current parent
   * @param key field name
   * @param type ES mapping type ({@code keyword}, {@code long} etc.)
   */
  private static void applyMapping(ObjectNode parent, String key, String type) {
    final int index = key.indexOf('.');
    if (index > -1) {
      String prefix  = key.substring(0, index);
      String suffix = key.substring(index + 1, key.length());
      applyMapping(parent.with(prefix).with("properties"), suffix, type);
    } else {
      parent.with(key).put("type", type);
    }
  }

  void insertDocument(ObjectNode document) throws IOException {
    Objects.requireNonNull(index, "index");
    Objects.requireNonNull(document, "document");
    String uri = String.format(Locale.ROOT, "/%s/_doc?refresh", index);
    StringEntity entity = new StringEntity(mapper().writeValueAsString(document),
            ContentType.APPLICATION_JSON);
    final Request r = new Request("POST", uri);
    r.setEntity(entity);
    restClient().performRequest(r);
  }

  void insertBulk(List<ObjectNode> documents) throws IOException {
    Objects.requireNonNull(documents, "documents");

    if (documents.isEmpty()) {
      // nothing to process
      return;
    }

    final List<String> bulk = new ArrayList<>(documents.size() * 2);
    for (ObjectNode doc: documents) {
      final ObjectNode header = mapper.createObjectNode();
      header.with("index").put("_index", index);
      if (doc.has("_id")) {
        // check if document has already _id
        header.with("index").set("_id", doc.get("_id"));
        doc.remove("_id");
      }

      bulk.add(header.toString());
      bulk.add(mapper().writeValueAsString(doc));
    }

    final StringEntity entity = new StringEntity(String.join("\n", bulk) + "\n",
            ContentType.APPLICATION_JSON);

    final Request r = new Request("POST", "/_bulk?refresh");
    r.setEntity(entity);
    restClient().performRequest(r);
  }

  <T> Function<JsonNode, T> jsonConverter(Class<T> type) {
    return json -> {
      try {
        return mapper.treeToValue(json, type);
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  <T> Function<Response, Json.Result> responseConverter() {
    return response -> {
      try (InputStream is = response.getEntity().getContent()) {
        return mapper.readValue(is, Json.Result.class);
      } catch (IOException e) {
        final String message = String.format("Couldn't parse HTTP response %s into %s", response, Json.Result.class.getSimpleName());
        throw new UncheckedIOException(message, e);
      }
    };
  }

  /**
   * Fetches next results given a scrollId.
   */
  Single<Json.Result> nextScroll(String scrollId) {
    // fetch next scroll
    final Request request = new Request("POST", "/_search/scroll");
    final ObjectNode payload = mapper.createObjectNode()
            .put("scroll", "1m")
            .put("scroll_id", scrollId);
    request.setJsonEntity(payload.toString());
    return rawHttp().apply(request).map(r -> responseConverter().apply(r));
  }

  Completable closeScroll(Iterable<String> scrollIds) {
    final ObjectNode payload = mapper.createObjectNode();
    final ArrayNode array = payload.withArray("scroll_id");
    scrollIds.forEach(array::add);
    final Request request = new Request("POST", "/_search/scroll");
    request.setJsonEntity(payload.toString());
    return rawHttp().apply(request).ignoreElement();
  }

  Function<ObjectNode, Single<Response>> search() {
    return search(Collections.emptyMap());
  }

  Function<ObjectNode, Single<Response>> search(Map<String, String> httpParams) {
    return query -> {
      final Request request = new Request("POST", String.format("/%s/_search", index));
      httpParams.forEach(request::addParameter);
      request.setJsonEntity(query.toString());
      return rawHttp().apply(request);
    };
  }

  Function<Request, Single<Response>> rawHttp() {
    return request -> Single.create(source -> {
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

  private ObjectMapper mapper() {
    return mapper;
  }

  private RestClient restClient() {
    return restClient;
  }


}
