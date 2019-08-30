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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.immutables.criteria.personmodel.CriteriaChecker;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Test for <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-scroll.html">scrolling functionality</a> of ES
 * @see Scrolling
 */
public class ScrollingTest {

  @ClassRule
  public static final EmbeddedElasticsearchResource RESOURCE = EmbeddedElasticsearchResource.create();

  private static final ObjectMapper MAPPER = ElasticPersonTest.MAPPER;

  /**
   * Should be greater than default elastic {@code size} which is 10.
   * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-from-size.html">from-size</a>
   */
  private static final int SIZE = 20;

  @BeforeClass
  public static void elasticseachInit() throws Exception {
    ElasticsearchBackend backend = backend();
    final ElasticsearchOps ops = new ElasticsearchOps(backend.restClient, "test", backend.objectMapper, 1024);

    Map<String, String> model = ImmutableMap.<String, String>builder()
            .put("string", "keyword")
            .put("optionalString", "keyword")
            .put("bool", "boolean")
            .put("intNumber", "integer")
            .build();

    ops.createIndex(model);
    for (int i = 0; i < SIZE; i++) {
      ObjectNode doc = MAPPER.createObjectNode()
              .put("string", "s" + i)
              .put("bool", true)
              .put("intNumber", i);

      ops.insertDocument(doc);
    }
  }

  private static ElasticsearchBackend backend() {
    return backend(1024);
  }

  private static ElasticsearchBackend backend(int scrollSize) {
    return new ElasticsearchBackend(ElasticsearchSetup.builder(RESOURCE.restClient()).objectMapper(MAPPER).resolver(ignore -> "test").scrollSize(scrollSize).build());
  }

  @Test
  public void noLimit() throws Exception {
    ElasticModelRepository repository = new ElasticModelRepository(backend(1024));

    CriteriaChecker.<ElasticModel>of(repository.findAll())
            .toList(ElasticModel::string)
            .hasSize(SIZE);

    CriteriaChecker.<ElasticModel>of(repository.findAll().orderBy(ElasticModelCriteria.elasticModel.string.asc()))
            .toList(ElasticModel::string)
            .hasSize(SIZE);

    // TODO scrolls are not cleared
    // assertNoActiveScrolls();
  }

  /**
   * Set scroll sizes like {@code 1, 2, 3 ...} and validates number of returned records
   */
  @Test
  public void withLimit() throws Exception {
    // set of scroll sizes / limits to tests
    final int[] samples = {1, 2, 3, SIZE - 1, SIZE, SIZE + 1, 2 * SIZE, SIZE * SIZE};
    for (int scrollSize: samples) {
      for (int limit: samples) {
        ElasticModelRepository repository = new ElasticModelRepository(backend(scrollSize));
        final int expected = Math.min(SIZE, limit);

        // with limit
        CriteriaChecker.<ElasticModel>of(repository.findAll().limit(limit))
                .toList(ElasticModel::string)
                .hasSize(expected);

        // sort expected results manually
        final List<String> expectedStrings = IntStream.range(0, SIZE)
                .mapToObj(i -> "s" + i)
                .sorted()
                .limit(expected)
                .collect(Collectors.toList());
        // add order by
        CriteriaChecker.<ElasticModel>of(repository.findAll().limit(limit)
                .orderBy(ElasticModelCriteria.elasticModel.string.asc()))
                .toList(ElasticModel::string)
                .isOf(expectedStrings);

        // TODO scrolls are not cleared
        // assertNoActiveScrolls();
      }
    }
  }

  /**
   * Ensures there are no pending scroll contexts in elastic search cluster.
   * Queries {@code /_nodes/stats/indices/search} endpoint.
   * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-stats.html">Indices Stats</a>
   */
  private void assertNoActiveScrolls() throws Exception {
    // get node stats
    final Response response = RESOURCE.restClient()
            .performRequest(new Request("GET", "/_nodes/stats/indices/search"));

    try (InputStream is = response.getEntity().getContent()) {
      final ObjectNode node = backend().objectMapper.readValue(is, ObjectNode.class);
      final String path = "/indices/search/scroll_current";
      final JsonNode scrollCurrent = node.with("nodes").elements().next().at(path);
      if (scrollCurrent.isMissingNode()) {
        throw new IllegalStateException("Couldn't find node at " + path);
      }

      final int activeScrolls = scrollCurrent.asInt();
      if (activeScrolls != 0) {
        throw new AssertionError(String.format("Expected no active scrolls but got %d. " +
                "Current index stats %s", activeScrolls, node));
      }
    }
  }

}
