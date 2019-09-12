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

import com.google.common.io.Closer;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.transport.TransportAddress;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.IOException;
import java.util.Objects;

class ElasticExtension implements ParameterResolver, AfterEachCallback {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(ElasticExtension.class);

  private static final String KEY = "elastic";

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return parameterContext.getParameter().getType() == RestClient.class;
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return getOrCreate(extensionContext).restClient();
  }

  private ElasticResource getOrCreate(ExtensionContext context) {
    return context.getStore(NAMESPACE).getOrComputeIfAbsent(KEY, key -> new ElasticResource(EmbeddedElasticsearchNode.create()), ElasticResource.class);
  }


  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    ElasticResource resource = context.getStore(NAMESPACE).get(KEY, ElasticResource.class);
    if (resource != null) {
      resource.clear();
    }
  }

  private static class ElasticResource implements ExtensionContext.Store.CloseableResource {

    private final EmbeddedElasticsearchNode node;
    private final Closer closer;
    private final RestClient client;

    private ElasticResource(EmbeddedElasticsearchNode node) {
      this.node = Objects.requireNonNull(node, "node");
      this.closer = Closer.create();
      closer.register(node);
      node.start();
      this.client = RestClient.builder(httpHost()).build();
      closer.register(client);
    }

    @Override
    public void close() throws Exception {
      closer.close();
    }

    /**
     * Delete all indexes for next test run
     */
    void clear() throws IOException {
      Request request = new Request("GET", "/_cat/indices/?h=index");
      String context = EntityUtils.toString(restClient().performRequest(request).getEntity());
      for (String index: context.split("\\n")) {
        restClient().performRequest(new Request("DELETE", "/" + index));
      }
    }

    /**
     * Low-level http rest client connected to current embedded elastic search instance.
     * @return http client connected to ES cluster
     */
    RestClient restClient() {
      return client;
    }

    private HttpHost httpHost() {
      final TransportAddress address = node.httpAddress();
      return new HttpHost(address.getAddress(), address.getPort());
    }

  }
}