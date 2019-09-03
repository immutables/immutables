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

import com.mongodb.MongoClientSettings;
import io.reactivex.Flowable;
import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.immutables.check.Checkers;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.personmodel.AbstractPersonTest;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Basic tests of mongo adapter
 */
public class MongoPersonTest extends AbstractPersonTest {

  private final MongoResource database = MongoResource.create();
  private final BackendResource backend = new BackendResource(database.database());

  @Rule
  public final RuleChain chain= RuleChain.outerRule(database).around(backend);

  @Override
  protected Set<Feature> features() {
    return EnumSet.of(Feature.DELETE, Feature.QUERY, Feature.QUERY_WITH_LIMIT,
            Feature.QUERY_WITH_PROJECTION,
            Feature.QUERY_WITH_OFFSET, Feature.ORDER_BY, Feature.REGEX,
            Feature.STRING_PREFIX_SUFFIX,
            Feature.ITERABLE_SIZE,
            Feature.ITERABLE_CONTAINS,
            Feature.STRING_LENGTH
    );
  }

  @Override
  protected Backend backend() {
    return backend.backend();
  }

  /**
   * Test that {@code _id} attribute is persisted instead of {@code id}
   */
  @Test
  public void idAttribute() {
    insert(new PersonGenerator().next().withId("id123").withAge(22));
    // query directly
    final List<BsonDocument> docs = Flowable.fromPublisher(backend.collection(Person.class)
            .withDocumentClass(BsonDocument.class)
            .withCodecRegistry(MongoClientSettings.getDefaultCodecRegistry())
            .find()).toList().blockingGet();

    Checkers.check(docs).hasSize(1);
    Checkers.check(docs.get(0).get("_id")).is(new BsonString("id123"));
    Checkers.check(docs.get(0).get("age")).is(new BsonInt32(22));

    // query using repository
    final List<Person> persons= repository.findAll().fetch();
    Checkers.check(persons).hasSize(1);
    Checkers.check(persons.get(0).id()).is("id123");
  }

  @Test
  public void jsr310() {
    insert(new PersonGenerator().next().withDateOfBirth(LocalDate.of(1990, 2, 2)));
    // query directly
    final List<BsonDocument> docs = Flowable.fromPublisher(backend.collection(Person.class)
            .withDocumentClass(BsonDocument.class)
            .withCodecRegistry(MongoClientSettings.getDefaultCodecRegistry())
            .find()).toList().blockingGet();

    Checkers.check(docs).hasSize(1);
    final LocalDate expected = LocalDate.of(1990, 2, 2);
    final long epochMillis = expected.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
    Checkers.check(docs.get(0).get("dateOfBirth")).is(new BsonDateTime(epochMillis));
  }

}
