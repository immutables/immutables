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
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.personmodel.AbstractPersonTest;
import org.immutables.criteria.personmodel.PersonAggregationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;

/**
 * Start embedded ES instance. Insert document(s) then find it.
 */
public class ElasticAggregationTest extends PersonAggregationTest {

  @ClassRule
  public static final EmbeddedElasticsearchResource ELASTIC = EmbeddedElasticsearchResource.create();

  static final ObjectMapper MAPPER = ElasticPersonTest.MAPPER;

  static final String INDEX_NAME = ElasticPersonTest.INDEX_NAME;

  private ElasticsearchBackend backend;

  private IndexOps ops;

  @Before
  public void setupRepository() throws Exception {
    ops = new IndexOps(ELASTIC.restClient(), MAPPER, INDEX_NAME);

    ops.create(PersonModel.MAPPING).blockingGet();

    this.backend = new ElasticsearchBackend(ElasticsearchSetup.builder(ELASTIC.restClient()).objectMapper(MAPPER).build());
  }

  @After
  public void tearDown() throws Exception {
    if (ops != null) {
      ops.delete().blockingGet();
    }
  }

  @Override
  protected Backend backend() {
    return backend;
  }
}
