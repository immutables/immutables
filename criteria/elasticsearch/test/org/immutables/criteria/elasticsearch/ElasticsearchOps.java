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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Helper methods to <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-put-mapping.html">define an index</a>
 * or insert documents in elastic search.
 */
public class ElasticsearchOps {

  private final RestClient restClient;
  private final ObjectMapper mapper;

  ElasticsearchOps(RestClient restClient, ObjectMapper mapper) {
    this.restClient = Objects.requireNonNull(restClient, "restClient");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
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
   * @param index index of the index
   * @param mapping field and field type mapping
   * @throws IOException if there is an error
   */
  void createIndex(String index, Map<String, String> mapping) throws IOException {
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

  void deleteIndex(String index) throws IOException {
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

  void insertDocument(String index, ObjectNode document) throws IOException {
    Objects.requireNonNull(index, "index");
    Objects.requireNonNull(document, "document");
    String uri = String.format(Locale.ROOT,
            "/%s/_doc?refresh", index);
    StringEntity entity = new StringEntity(mapper().writeValueAsString(document),
            ContentType.APPLICATION_JSON);
    final Request r = new Request("POST", uri);
    r.setEntity(entity);
    restClient().performRequest(r);
  }

  void insertBulk(String index, List<ObjectNode> documents) throws IOException {
    Objects.requireNonNull(index, "index");
    Objects.requireNonNull(documents, "documents");

    if (documents.isEmpty()) {
      // nothing to process
      return;
    }

    List<String> bulk = new ArrayList<>(documents.size() * 2);
    for (ObjectNode doc: documents) {
      bulk.add(String.format("{\"index\": {\"_index\":\"%s\"}}", index));
      bulk.add(mapper().writeValueAsString(doc));
    }

    final StringEntity entity = new StringEntity(String.join("\n", bulk) + "\n",
            ContentType.APPLICATION_JSON);

    final Request r = new Request("POST", "/_bulk?refresh");
    r.setEntity(entity);
    restClient().performRequest(r);
  }

  private ObjectMapper mapper() {
    return mapper;
  }

  private RestClient restClient() {
    return restClient;
  }


}
