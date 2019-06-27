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
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Observable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Start embedded ES instance. Insert document(s) then find it.
 */
public class ElasticModelTest {

  @ClassRule
  public static final EmbeddedElasticsearchResource RESOURCE = EmbeddedElasticsearchResource.create();

  private static final ObjectMapper MAPPER = new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .registerModule(new Jdk8Module());

  private static final String INDEX_NAME = "mymodel";

  private ElasticModelRepository repository;

  @BeforeClass
  public static void setupElastic() throws Exception {
    final ElasticsearchOps ops = new ElasticsearchOps(RESOURCE.restClient(), INDEX_NAME, new ObjectMapper());

    Map<String, String> model = ImmutableMap.<String, String>builder()
            .put("string", "keyword")
            .put("optionalString", "keyword")
            .put("bool", "boolean")
            .put("intNumber", "integer")
            .build();

    ops.createIndex(model);

    ObjectNode doc1 = MAPPER.createObjectNode()
                .put("string", "foo")
                .put("optionalString", "optFoo")
                .put("bool", true)
                .put("intNumber", 42);

    ObjectNode doc2 = MAPPER.createObjectNode()
                .put("string", "bar")
                .put("optionalString", "optBar")
                .put("bool", false)
                .put("intNumber", 44);

    ops.insertBulk(Arrays.asList(doc1, doc2));
  }

  @Before
  public void setupRepository() throws Exception {
    ElasticsearchBackend backend = new ElasticsearchBackend(RESOURCE.restClient(), MAPPER, INDEX_NAME);
    this.repository = new ElasticModelRepository(backend);
  }

  @Test
  public void criteria() {
    ElasticModelCriteria<ElasticModelCriteria.Self> crit = ElasticModelCriteria.create();

    assertCount(crit, 2);
    assertCount(crit.intNumber.isEqualTo(1), 0);
    assertCount(crit.string.isEqualTo("foo"), 1);
    assertCount(crit.string.isIn("foo", "bar"), 2);
    assertCount(crit.string.isNotIn("foo", "bar"), 0);
    assertCount(crit.string.isIn("foo", "foo2"), 1);
    assertCount(crit.string.isIn("not", "not"), 0);
    assertCount(crit.string.isEqualTo("bar"), 1);
    assertCount(crit.string.isEqualTo("hello"), 0);
    assertCount(crit.optionalString.value().isEqualTo("optFoo"), 1);
    assertCount(crit.optionalString.value().isEqualTo("missing"), 0);
    assertCount(crit.intNumber.isAtMost(42).string.isEqualTo("foo"), 1);
    assertCount(crit.intNumber.isAtMost(11), 0);

  }

  private void assertCount(ElasticModelCriteria<?> crit, int count) {
    Observable.fromPublisher(repository.find(crit).fetch())
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertValueCount(count);
  }

}
