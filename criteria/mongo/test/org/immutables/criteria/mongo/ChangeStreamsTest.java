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

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.WatchEvent;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.immutables.criteria.personmodel.PersonRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.immutables.check.Checkers.check;

/**
 * Validate <a href="https://docs.mongodb.com/manual/changeStreams/">change streams</a> functionality.
 * Requires real mongo instance.
 *
 * <p>To connect to external mongo database use system property {@code mongo}.
 * </pre>
 *
 * <p>See <a href="https://gist.github.com/oleurud/d9629ef197d8dab682f9035f4bb29065">Local mongo replicaset with docker</a>
 * to start mongo with change stream support. Then run tests with maven or IDE.
 * <h4>Maven</h4>
 * <pre>
 * {@code $ mvn test -DargLine="-Dmongo=mongodb://localhost:27017,localhost:27018,localhost:27019/testDB?replicaSet=my-mongo-set"}
 * </pre>
 *
 *
 */
@ExtendWith(MongoExtension.class)
class ChangeStreamsTest {

  private final PersonRepository repository;
  private final PersonCriteria person;
  private final PersonGenerator generator;

  ChangeStreamsTest(MongoInstance instance) {
    Assumptions.assumeTrue(instance.isRealMongo(), "This test requires real mongo instance");
    Backend backend = new BackendResource(instance.database()).backend();
    this.repository = new PersonRepository(backend);
    this.person = PersonCriteria.person;
    this.generator = new PersonGenerator();
  }

  @Test
  void basic() {
    Flowable<WatchEvent<Person>> flowable = Flowable.fromPublisher(repository.watcher(person).watch());
    TestSubscriber<WatchEvent<Person>> test = flowable.test();
    repository.insert(generator.next().withId("id1").withFullName("p1"));
    test.awaitCount(1).assertValueCount(1);
    test.assertNotComplete();
    WatchEvent<Person> event = test.values().get(0);
    check(event.key()).is("id1");
    check(event.operation()).is(WatchEvent.Operation.INSERT);
    check(event.newValue().isPresent());
    check(event.newValue().get().id()).is("id1");
    check(event.newValue().get().fullName()).is("p1");
  }

  /**
   * Watch event with filter
   */
  @Test
  void filter() throws InterruptedException {
    PersonCriteria filter = person.age.atLeast(21);
    Flowable<WatchEvent<Person>> flowable = Flowable.fromPublisher(repository.watcher(filter).watch());
    TestSubscriber<WatchEvent<Person>> test = flowable.test();
    repository.insert(generator.next().withId("id1").withAge(18));
    test.await(200, TimeUnit.MILLISECONDS);
    test.assertEmpty();
    repository.insert(generator.next().withId("id2").withAge(19));
    test.await(200, TimeUnit.MILLISECONDS);
    test.assertEmpty();
    repository.insert(generator.next().withId("id3").withAge(21));
    test.awaitCount(1).assertValueCount(1);
    Person person = test.values().get(0).newValue().get();
    check(person.id()).is("id3");
    check(person.age()).is(21);
  }

  @Test
  void multiFilter() throws InterruptedException {
    PersonCriteria filter = person.age.atLeast(21).isActive.isTrue();
    Flowable<WatchEvent<Person>> flowable = Flowable.fromPublisher(repository.watcher(filter).watch());
    TestSubscriber<WatchEvent<Person>> test = flowable.test();
    repository.insert(generator.next().withId("id1").withAge(19));
    test.await(200, TimeUnit.MILLISECONDS);
    test.assertEmpty();

    repository.insert(generator.next().withId("id2").withAge(21).withIsActive(false));
    test.await(200, TimeUnit.MILLISECONDS);
    test.assertEmpty();

    repository.insert(generator.next().withId("id3").withAge(21).withIsActive(true));
    test.awaitCount(1).assertValueCount(1);
    Person person = test.values().get(0).newValue().get();
    check(person.id()).is("id3");
  }
}
