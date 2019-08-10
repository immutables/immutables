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

  private static final ObjectMapper MAPPER = ElasticPersonTest.MAPPER;

  private static final String INDEX_NAME = "mymodel";

  private ElasticModelRepository repository;

  @BeforeClass
  public static void setupElastic() throws Exception {
    final ElasticsearchOps ops = new ElasticsearchOps(RESOURCE.restClient(), INDEX_NAME, MAPPER, 1024);

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
    ElasticsearchBackend backend = new ElasticsearchBackend(RESOURCE.restClient(), MAPPER, x -> INDEX_NAME);
    this.repository = new ElasticModelRepository(backend);
  }

  @Test
  public void criteria() {
    ElasticModelCriteria crit = ElasticModelCriteria.elasticModel;

    assertCount(crit, 2);
    assertCount(crit.intNumber.is(1), 0);
    assertCount(crit.string.is("foo"), 1);
    assertCount(crit.string.in("foo", "bar"), 2);
    assertCount(crit.string.notIn("foo", "bar"), 0);
    assertCount(crit.string.in("foo", "foo2"), 1);
    assertCount(crit.string.in("not", "not"), 0);
    assertCount(crit.string.is("bar"), 1);
    assertCount(crit.string.is("hello"), 0);
    assertCount(crit.optionalString.value().is("optFoo"), 1);
    assertCount(crit.optionalString.value().is("missing"), 0);
    assertCount(crit.intNumber.atMost(42).string.is("foo"), 1);
    assertCount(crit.intNumber.atMost(11), 0);

  }

  private void assertCount(ElasticModelCriteria crit, int count) {
    Observable.fromPublisher(repository.find(crit).fetch())
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertValueCount(count);
  }

}
