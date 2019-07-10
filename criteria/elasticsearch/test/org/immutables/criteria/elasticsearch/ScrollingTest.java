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
import com.google.common.collect.ImmutableMap;
import org.immutables.criteria.personmodel.CriteriaChecker;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

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
    final ElasticsearchOps ops = ops();

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

  private static ElasticsearchOps ops() {
    return ops(1024);
  }

  private static ElasticsearchOps ops(int scrollSize) {
    return new ElasticsearchOps(RESOURCE.restClient(), "test", MAPPER, scrollSize);
  }

  @Test
  public void noLimit() {
    ElasticModelRepository repository = new ElasticModelRepository(new ElasticsearchBackend(ops(1)));

    CriteriaChecker.of(repository.findAll())
            .toList(ElasticModel::string)
            .hasSize(SIZE);

    CriteriaChecker.of(repository.findAll().orderBy(ElasticModelCriteria.elasticModel.string.asc()))
            .toList(ElasticModel::string)
            .hasSize(SIZE);
  }

  /**
   * Set scroll sizes like {@code 1, 2, 3 ...} and validates number of returned records
   */
  @Test
  public void withLimit() {
    // set of scroll sizes / limits to tests
    final int[] samples = {1, 2, 3, SIZE - 1, SIZE, SIZE + 1, 2 * SIZE, SIZE * SIZE};
    for (int scrollSize: samples) {
      for (int limit: samples) {
        ElasticModelRepository repository = new ElasticModelRepository(new ElasticsearchBackend(ops(scrollSize)));
        final int expected = Math.min(SIZE, limit);

        // with limit
        CriteriaChecker.of(repository.findAll().limit(limit))
                .toList(ElasticModel::string)
                .hasSize(expected);

        // sort expected results manually
        final List<String> expectedStrings = IntStream.range(0, SIZE)
                .mapToObj(i -> "s" + i)
                .sorted()
                .limit(expected)
                .collect(Collectors.toList());
        // add order by
        CriteriaChecker.of(repository.findAll().limit(limit)
                .orderBy(ElasticModelCriteria.elasticModel.string.asc()))
                .toList(ElasticModel::string)
                .isOf(expectedStrings);
      }
    }
  }
}
