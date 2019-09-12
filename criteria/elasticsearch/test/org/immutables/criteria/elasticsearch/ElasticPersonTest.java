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
import org.elasticsearch.client.RestClient;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.ContainerNaming;
import org.immutables.criteria.personmodel.AbstractPersonTest;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Start embedded ES instance. Insert document(s) then find it.
 */
@ExtendWith(ElasticExtension.class)
public class ElasticPersonTest extends AbstractPersonTest  {

  private final Backend backend;
  private final IndexOps ops;

  static final ObjectMapper MAPPER = new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .registerModule(new GuavaModule())
          .registerModule(new Jdk8Module());

  static final String INDEX_NAME = ContainerNaming.DEFAULT.name(Person.class);

  ElasticPersonTest(RestClient restClient) throws IOException {
    ops = new IndexOps(restClient, MAPPER, INDEX_NAME);
    ops.create(PersonModel.MAPPING).blockingGet();
    this.backend = new ElasticsearchBackend(ElasticsearchSetup.builder(restClient).objectMapper(MAPPER).build());
  }

  /**
   * Elastic has special <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/regexp-syntax.html">regex syntax</a>
   * and not all java patterns are supported. Testing separately here
   */
  @Test
  public void regex_forElastic() {
    final PersonGenerator generator = new PersonGenerator();
    insert(generator.next().withFullName("John"));

    check(repository().find(person.fullName.matches(Pattern.compile("John")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile("J.*n")))).hasSize(1);
    check(repository().find(person.fullName.not(s ->s.matches(Pattern.compile("J.*n"))))).empty();
    check(repository().find(person.fullName.matches(Pattern.compile("J..n")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile("J...")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile("...n")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile(".*")))).hasSize(1);

    insert(generator.next().withFullName("Mary"));
    check(repository().find(person.fullName.matches(Pattern.compile("J.*n")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile("M.*ry")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile(".*")))).hasSize(2);
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
