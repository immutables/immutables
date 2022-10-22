/*
 * Copyright 2022 Immutables Authors and Contributors
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

import com.google.common.collect.ImmutableList;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.personmodel.ImmutablePerson;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.immutables.criteria.personmodel.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.immutables.check.Checkers.check;

/**
 * Validate results of a mongo write operation with {@link WriteResult}.
 */
@ExtendWith(MongoExtension.class)
class MongoWriteResultTest {
  private final PersonRepository repository;
  private final PersonCriteria person;

  private final PersonGenerator generator;

  MongoWriteResultTest(MongoDatabase database) {
    BackendResource backend = new BackendResource(database);
    this.repository = new PersonRepository(backend.backend());
    this.person = PersonCriteria.person;
    this.generator = new PersonGenerator();
  }

  @Test
  void insert() {
    WriteResult result = repository.insert(generator.next());
    check(result.insertedCount().getAsLong()).is(1L);
    check(result.deletedCount().getAsLong()).is(0L);
    check(result.updatedCount().getAsLong()).is(0L);

    WriteResult result2 = repository.insertAll(ImmutableList.of(generator.next(), generator.next()));
    check(result2.insertedCount().getAsLong()).is(2L);
    check(result2.deletedCount().getAsLong()).is(0L);
    check(result2.updatedCount().getAsLong()).is(0L);
  }

  @Test
  void delete() {
    WriteResult result = repository.delete(person.id.is("__missing__"));
    check(result.insertedCount().getAsLong()).is(0L);
    check(result.deletedCount().getAsLong()).is(0L);
    check(result.updatedCount().getAsLong()).is(0L);
    check(result.totalCount().getAsLong()).is(0L);

    Person person1 = generator.next();
    repository.insert(person1);
    WriteResult result2 = repository.delete(person.id.is(person1.id()));
    check(result2.deletedCount().getAsLong()).is(1L);
  }

  @Test
  void update() {
    WriteResult result1 = repository.update(person.id.is("aaa")).set(person.fullName, "name1").execute();
    check(result1.updatedCount().getAsLong()).is(0L);
    check(result1.insertedCount().getAsLong()).is(0L);
    check(result1.deletedCount().getAsLong()).is(0L);

    ImmutablePerson person1 = generator.next();
    repository.insert(person1);

    WriteResult result2 = repository.update(person.id.is(person1.id())).set(person.fullName, "name2").execute();
    check(result2.updatedCount().getAsLong()).is(1L);
    check(result2.insertedCount().getAsLong()).is(0L);
    check(result2.deletedCount().getAsLong()).is(0L);
  }
}
