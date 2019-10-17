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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Completable;
import io.reactivex.Single;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Elastic index operations to create, delete an index.
 */
class IndexOps {

  private final RxJavaTransport transport;
  private final ObjectMapper mapper;
  private final String index;

  IndexOps(RestClient restClient, ObjectMapper mapper, String index) {
    this.transport = new RxJavaTransport(restClient);
    this.mapper = mapper;
    this.index = Objects.requireNonNull(index, "index");
  }

  Single<Boolean> exists() {
    final String uri = String.format(Locale.ROOT, "/%s/_mapping", index);
    final Request request = new Request("GET", uri);
    return transport.execute(request).map(x -> true).onErrorResumeNext(e -> Single.just(false));
  }

  /**
   * Return mapping for current index
   */
  Single<Mapping> mapping() {
    final String uri = String.format(Locale.ROOT, "/%s/_mapping", index);
    final Request request = new Request("GET", uri);
    return transport.execute(request)
            .map(response -> mapper.readValue(response.getEntity().getContent(), ObjectNode.class))
            .map(root -> {
              ObjectNode properties = (ObjectNode) root.elements().next().get("mappings");

              if (properties == null) {
                throw new IllegalStateException(String.format("No mappings found for index %s (after request %s)", index, request));
              }

              ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
              Json.visitMappingProperties(properties, builder::put);
              return Mapping.ofElastic(builder.build());
            });
  }

  Single<Version> version() {
    final Request request = new Request("GET", "/");

    // version extract function
    final Function<ObjectNode, Version> fn = node -> Version.of(node.get("version").get("number").asText());
    return transport.execute(request)
            .map(response -> mapper.readValue(response.getEntity().getContent(), ObjectNode.class))
            .map(fn::apply);
  }

  /**
   * Creates elastic search mapping (index) on the cluster
   */
  Completable create(Mapping mapping) {
    Objects.requireNonNull(mapping, "mapping");
    Map<String, String> newMap =  mapping.fields()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().name()));

    return create(newMap);
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
  Completable create(Map<String, String> mapping) {
    Objects.requireNonNull(mapping, "mapping");

    ObjectNode mappings = mapper.createObjectNode();

    ObjectNode properties = mappings.with("mappings").with("properties");
    for (Map.Entry<String, String> entry: mapping.entrySet()) {
      applyMapping(properties, entry.getKey(), entry.getValue());
    }

    // create index and mapping
    try {
      final HttpEntity entity = new StringEntity(mapper.writeValueAsString(mappings),
              ContentType.APPLICATION_JSON);
      final Request r = new Request("PUT", "/" + index);
      r.setEntity(entity);
      return transport.execute(r).ignoreElement();
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
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
      String suffix = key.substring(index + 1);
      applyMapping(parent.with(prefix).with("properties"), suffix, type);
    } else {
      parent.with(key).put("type", type);
    }
  }

  Completable delete() {
    final Request r = new Request("DELETE", "/" + index);
    return transport.execute(r).ignoreElement();
  }

}
