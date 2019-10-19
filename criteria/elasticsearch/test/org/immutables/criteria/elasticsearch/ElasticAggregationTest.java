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
import com.google.common.base.Throwables;
import org.elasticsearch.client.RestClient;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.personmodel.PersonAggregationTest;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

/**
 * Start embedded ES instance. Insert document(s) then find it.
 */
@ExtendWith(ElasticExtension.class)
public class ElasticAggregationTest extends PersonAggregationTest {

  private static final ObjectMapper MAPPER = ElasticPersonTest.MAPPER;

  private static final String INDEX_NAME = ElasticPersonTest.INDEX_NAME;

  private final ElasticsearchBackend backend;

  ElasticAggregationTest(RestClient restClient) throws IOException {
    IndexOps ops = new IndexOps(restClient, MAPPER, INDEX_NAME);
    ops.create(PersonModel.MAPPING).blockingAwait();
    this.backend = new ElasticsearchBackend(ElasticsearchSetup.builder(restClient).objectMapper(MAPPER).build());
  }

  @Override
  protected Backend backend() {
    return backend;
  }
}
