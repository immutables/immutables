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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.personmodel.AbstractPersonTest;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Start embedded ES instance. Insert document(s) then find it.
 */
public class ElasticPersonTest extends AbstractPersonTest  {

  @ClassRule
  public static final EmbeddedElasticsearchResource ELASTIC = EmbeddedElasticsearchResource.create();

  static final ObjectMapper MAPPER = new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .registerModule(new GuavaModule())
          .registerModule(new Jdk8Module());

  private static final String INDEX_NAME = "persons";

  private ElasticsearchBackend backend;

  private ElasticsearchOps ops;

  @Before
  public void setupRepository() throws Exception {
    ops = new ElasticsearchOps(ELASTIC.restClient(), INDEX_NAME, MAPPER, 1024);

    // person model
    // TODO automatically generate it from Class
    Map<String, String> model = ImmutableMap.<String, String>builder()
            .put("id", "keyword")
            .put("isActive", "boolean")
            .put("fullName", "keyword")
            .put("nickName", "keyword")
            .put("age", "integer")
            .put("dateOfBirth", "date")
            .put("address.street", "keyword")
            .put("address.state", "keyword")
            .put("address.zip", "keyword")
            .put("address.city", "keyword")
            .build();

    ops.createIndex(model);

    this.backend = new ElasticsearchBackend(ElasticsearchSetup.builder(ELASTIC.restClient()).objectMapper(MAPPER).resolver(ignore -> INDEX_NAME).build());
  }

  @After
  public void tearDown() throws Exception {
    if (ops != null) {
      ops.deleteIndex();
    }
  }

  /**
   * Elastic has special <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/regexp-syntax.html">regex syntax</a>
   * and not all java patterns are supported. Testing separately here
   */
  @Test
  public void regex_forElastic() {
    final PersonGenerator generator = new PersonGenerator();
    insert(generator.next().withFullName("John"));

    check(repository().find(criteria().fullName.matches(Pattern.compile("John")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile("J.*n")))).hasSize(1);
    check(repository().find(criteria().fullName.not(s ->s.matches(Pattern.compile("J.*n"))))).empty();
    check(repository().find(criteria().fullName.matches(Pattern.compile("J..n")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile("J...")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile("...n")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile(".*")))).hasSize(1);

    insert(generator.next().withFullName("Mary"));
    check(repository().find(criteria().fullName.matches(Pattern.compile("J.*n")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile("M.*ry")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile(".*")))).hasSize(2);
  }


  @Override
  protected Set<Feature> features() {
    return EnumSet.of(Feature.DELETE, Feature.QUERY, Feature.QUERY_WITH_LIMIT,
            Feature.QUERY_WITH_PROJECTION,
            Feature.QUERY_WITH_OFFSET, Feature.ORDER_BY, Feature.STRING_PREFIX_SUFFIX);
  }

  @Override
  protected Backend backend() {
    return backend;
  }
}
