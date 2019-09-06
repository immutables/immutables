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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.immutables.criteria.backend.WriteResult;

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
 * Helper methods to operate on ES index: search, scroll etc.
 */
class ElasticsearchOps {

  private final RxJavaTransport transport;
  private final ObjectMapper mapper;
  private final String index;
  final Mapping mapping;
  final Version version;

  /**
   * batch size of scroll
   */
  final int scrollSize;

  ElasticsearchOps(RestClient restClient, String index, ObjectMapper mapper, int scrollSize) {
    this.transport = new RxJavaTransport(restClient);
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.index = Objects.requireNonNull(index, "index");
    Preconditions.checkArgument(scrollSize > 0, "Invalid scrollSize: %s", scrollSize);
    this.scrollSize = scrollSize;
    IndexOps ops = new IndexOps(restClient, mapper, index);
    // cache mapping and version
    this.mapping = ops.mapping().blockingGet();
    this.version = ops.version().blockingGet();
  }

  Single<WriteResult> insertDocument(ObjectNode document) throws IOException {
    Objects.requireNonNull(document, "document");
    String uri = String.format(Locale.ROOT, "/%s/_doc?refresh", index);
    StringEntity entity = new StringEntity(mapper().writeValueAsString(document),
            ContentType.APPLICATION_JSON);
    final Request r = new Request("POST", uri);
    r.setEntity(entity);
    return transport.execute(r).map(x -> WriteResult.unknown());
  }

  Single<WriteResult> insertBulk(List<ObjectNode> documents) {
    return Single.defer(() -> insertBulkInternal(documents));
  }

  private Single<WriteResult> insertBulkInternal(List<ObjectNode> documents) throws JsonProcessingException {
    Objects.requireNonNull(documents, "documents");

    if (documents.isEmpty()) {
      // nothing to process
      return Single.just(WriteResult.empty());
    }

    final List<String> bulk = new ArrayList<>(documents.size() * 2);
    for (ObjectNode doc: documents) {
      final ObjectNode header = mapper.createObjectNode();
      header.with("index").put("_index", index);
      if (doc.has("_id")) {
        // check if document has already an _id
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
    return transport.execute(r).map(x -> WriteResult.unknown());
  }


  private <T> T convert(Response response, Class<T> type) {
    try (InputStream is = response.getEntity().getContent()) {
      return mapper.readValue(is, type);
    } catch (IOException e) {
      final String message = String.format("Couldn't parse HTTP response %s into %s", response, type.getSimpleName());
      throw new UncheckedIOException(message, e);
    }
  }

  private Function<Response, Json.Result> responseConverter() {
    return response -> convert(response, Json.Result.class);
  }

  /**
   * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-scroll.html">Scroll API</a>
   */
  <T> Flowable<T> scrolledSearch(ObjectNode query, JsonConverter<T> converter) {
    return new Scrolling<>(this, converter).scroll(query);
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
    return transport.execute(request).map(r -> responseConverter().apply(r));
  }

  Completable closeScroll(Iterable<String> scrollIds) {
    final ObjectNode payload = mapper.createObjectNode();
    final ArrayNode array = payload.withArray("scroll_id");
    scrollIds.forEach(array::add);
    final Request request = new Request("DELETE", "/_search/scroll");
    request.setJsonEntity(payload.toString());
    return transport.execute(request).ignoreElement();
  }

  <T> Flowable<T> search(ObjectNode query, JsonConverter<T> converter) {
    return searchRaw(query, Collections.emptyMap()).toFlowable()
            .flatMapIterable(r -> r.searchHits().hits())
            .map(x -> converter.convert(x.source()));
  }

  Single<Json.Result> searchRaw(ObjectNode query, Map<String, String> httpParams) {
      final Request request = new Request("POST", String.format("/%s/_search", index));
      httpParams.forEach(request::addParameter);
      request.setJsonEntity(query.toString());
      return transport.execute(request).map(r -> responseConverter().apply(r));
  }

  ObjectMapper mapper() {
    return mapper;
  }

}
