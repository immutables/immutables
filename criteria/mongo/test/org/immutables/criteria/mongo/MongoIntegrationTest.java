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

package org.immutables.criteria.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.reactivex.Flowable;
import org.immutables.criteria.DocumentCriteria;
import org.immutables.criteria.mongo.bson4jackson.JacksonCodecs;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.immutables.criteria.personmodel.PersonRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Basic tests of mongo adapter
 */
public class MongoIntegrationTest {


  private final MongoServer server = new MongoServer(new MemoryBackend());
  private final MongoClient client = MongoClients.create(String.format("mongodb://localhost:%d", server.bind().getPort()));

  private MongoCollection<Person> collection;

  private MongoBackend backend;
  private PersonRepository repository;

  @Before
  public void setUp() throws Exception {
    final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()) // need to support native BSON codecs
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module());

    Flowable.fromPublisher(client.getDatabase("test").createCollection("test"))
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertComplete();


    this.collection = client.getDatabase("test").getCollection("test")
            .withDocumentClass(Person.class)
            .withCodecRegistry(JacksonCodecs.registryFromMapper(mapper));

    this.backend = new MongoBackend(this.collection);
    this.repository = new PersonRepository(backend);
    final Person person = new PersonGenerator().next().withFullName("test").withAge(22);

    Flowable.fromPublisher(repository.insert(person))
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertComplete();
  }

  @After
  public void tearDown() throws Exception {
    client.close();
    server.shutdownNow();
  }

  @Test
  public void basic() {
    execute(PersonCriteria.create().fullName.isEqualTo("test"), 1);
    execute(PersonCriteria.create().fullName.isNotEqualTo("test"), 0);
    execute(PersonCriteria.create().fullName.isEqualTo("test")
            .age.isNotEqualTo(1), 1);
    execute(PersonCriteria.create().fullName.isEqualTo("_MISSING_"), 0);
    execute(PersonCriteria.create().fullName.isIn("test", "test2"), 1);
    execute(PersonCriteria.create().fullName.isNotIn("test", "test2"), 0);
  }

  @Test
  public void comparison() {
    execute(PersonCriteria.create().age.isAtLeast(22), 1);
    execute(PersonCriteria.create().age.isGreaterThan(22), 0);
    execute(PersonCriteria.create().age.isLessThan(22), 0);
    execute(PersonCriteria.create().age.isAtMost(22), 1);
  }

  private void execute(DocumentCriteria<Person> expr, int count) {
    Flowable.fromPublisher(repository.find(expr).fetch())
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertValueCount(count);

  }
}
