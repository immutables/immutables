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
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.reactivex.Flowable;
import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.jsr310.Jsr310CodecProvider;
import org.immutables.criteria.mongo.bson4jackson.IdAnnotationModule;
import org.immutables.criteria.mongo.bson4jackson.JacksonCodecs;
import org.immutables.criteria.personmodel.AbstractPersonTest;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.immutables.criteria.personmodel.PersonRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.immutables.check.Checkers.check;

/**
 * Basic tests of mongo adapter
 */
public class MongoPersonTest extends AbstractPersonTest {

  private static final String COLLECTION_NAME = "test";

  @Rule
  public final MongoResource MONGO = MongoResource.create();

  private MongoCollection<Person> collection;

  private MongoBackend backend;
  private PersonRepository repository;

  @Before
  public void setUp() throws Exception {
    final ObjectMapper mapper = new ObjectMapper()
            .registerModule(JacksonCodecs.module(new Jsr310CodecProvider()))
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module())
            .registerModule(new IdAnnotationModule());

    final MongoDatabase database = MONGO.database();

    Flowable.fromPublisher(database.createCollection(COLLECTION_NAME))
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertComplete();

    CodecRegistry registry = JacksonCodecs.registryFromMapper(mapper);
    this.collection = database.getCollection(COLLECTION_NAME)
            .withDocumentClass(Person.class)
            .withCodecRegistry(registry);

    this.backend = new MongoBackend(x -> collection);
    this.repository = new PersonRepository(backend);
  }

  /**
   * Test that {@code _id} attribute is persisted instead of {@code id}
   */
  @Test
  public void idAttribute() {
    insert(new PersonGenerator().next().withId("id123").withAge(22));
    // query directly
    final List<BsonDocument> docs = Flowable.fromPublisher(collection
            .withDocumentClass(BsonDocument.class)
            .withCodecRegistry(MongoClientSettings.getDefaultCodecRegistry())
            .find()).toList().blockingGet();

    check(docs).hasSize(1);
    check(docs.get(0).get("_id")).is(new BsonString("id123"));
    check(docs.get(0).get("age")).is(new BsonInt32(22));

    // query using repository
    final List<Person> persons= Flowable.fromPublisher(repository.findAll().fetch()).toList().blockingGet();
    check(persons).hasSize(1);
    check(persons.get(0).id()).is("id123");
  }

  @Test
  public void jsr310() {
    insert(new PersonGenerator().next().withDateOfBirth(LocalDate.of(1990, 2, 2)));
    // query directly
    final List<BsonDocument> docs = Flowable.fromPublisher(collection
            .withDocumentClass(BsonDocument.class)
            .withCodecRegistry(MongoClientSettings.getDefaultCodecRegistry())
            .find()).toList().blockingGet();

    check(docs).hasSize(1);
    final LocalDate expected = LocalDate.of(1990, 2, 2);
    final long epochMillis = expected.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
    check(docs.get(0).get("dateOfBirth")).is(new BsonDateTime(epochMillis));
  }


  @Override
  protected Set<Feature> features() {
    return EnumSet.of(Feature.DELETE, Feature.QUERY, Feature.QUERY_WITH_LIMIT, Feature.QUERY_WITH_OFFSET, Feature.ORDER_BY);
  }

  @Override
  protected PersonRepository repository() {
    return repository;
  }
}
