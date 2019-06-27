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
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import org.immutables.criteria.personmodel.AbstractPersonTest;
import org.immutables.criteria.personmodel.PersonRepository;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Start embedded ES instance. Insert document(s) then find it.
 */
@Ignore("doesn't work yet")
public class ElasticIntegrationTest extends AbstractPersonTest  {

  @ClassRule
  public static final EmbeddedElasticsearchResource RESOURCE = EmbeddedElasticsearchResource.create();

  private static final ObjectMapper MAPPER = new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .registerModule(new Jdk8Module());

  private static final String INDEX_NAME = "persons";

  private PersonRepository repository;

  @BeforeClass
  public static void setupElastic() throws Exception {
    final ElasticsearchOps ops = new ElasticsearchOps(RESOURCE.restClient(), INDEX_NAME, new ObjectMapper());

    // person
    Map<String, String> model = ImmutableMap.<String, String>builder()
            .put("id", "keyword")
            .put("isActive", "boolean")
            .put("fullName", "keyword")
            .put("nickName", "keyword")
            .put("age", "integer")
            .put("dateOfBirth", "date")
            .build();

    ops.createIndex(model);
  }

  @Before
  public void setupRepository() throws Exception {
    final ElasticsearchBackend backend = new ElasticsearchBackend(RESOURCE.restClient(), MAPPER, INDEX_NAME);
    this.repository = new PersonRepository(backend);
    populate();
  }

  @Override
  protected Set<Feature> features() {
    return EnumSet.of(Feature.DELETE, Feature.QUERY, Feature.QUERY_WITH_LIMIT, Feature.QUERY_WITH_OFFSET);
  }

  @Override
  protected PersonRepository repository() {
    return repository;
  }
}
