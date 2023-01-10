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
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
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
 * Requires real mongo instance since out integration testing
 * <a href="https://github.com/bwaldvogel/mongo-java-server">mongo-java-server</a> does not yet support
 * change streams.
 *
 * <p>To connect to external mongo database use system property {@code mongo}.
 * </pre>
 *
 * <h3>Docker Setup</h3>
 * <pre>
 * {@code
 * # pull the official mongo docker container
 * $ docker pull mongo
 *
 * # create network
 * $ docker network create my-mongo-cluster
 *
 * # Remove previous instances (if they exists)
 * $ docker rm -f mongo1 mongo2 mongo3
 *
 * # create mongos
 * $ docker run -d --net my-mongo-cluster -p 27017:27017 --name mongo1 mongo mongod --replSet my-mongo-set --port 27017
 * $ docker run -d --net my-mongo-cluster -p 27018:27018 --name mongo2 mongo mongod --replSet my-mongo-set --port 27018
 * $ docker run -d --net my-mongo-cluster -p 27019:27019 --name mongo3 mongo mongod --replSet my-mongo-set --port 27019
 *
 * # Update /etc/hosts
 * 127.0.0.1       mongo1 mongo2 mongo3
 *
 * # setup replica set
 * $ docker exec -it mongo1 mongosh
 * test> db = (new Mongo('localhost:27017')).getDB('test')
 * test> config={"_id":"my-mongo-set","members":[{"_id":0,"host":"mongo1:27017"},{"_id":1,"host":"mongo2:27018"},{"_id":2,"host":"mongo3:27019"}]}
 * test> rs.initiate(config)
 * { ok: 1 }
 * }
 * </pre>
 * <h3>Maven Test</h3>
 * Manually run the following maven command:
 *
 * <pre>
 * {@code $ mvn test -DargLine="-Dmongo=mongodb://localhost:27017,localhost:27018,localhost:27019/testDB?replicaSet=my-mongo-set"}
 * </pre>
 */
@ExtendWith(MongoExtension.class)
class ChangeStreamsTest {

  private final PersonRepository repository;
  private final PersonCriteria person;
  private final PersonGenerator generator;

  ChangeStreamsTest(MongoInstance instance) {
    Assumptions.assumeTrue(instance.isRealMongo(), "This test requires real mongo instance");
    Assumptions.assumeTrue(replicaSetEnabled(instance), "This test requires ReplicaSet enabled");
    Backend backend = new BackendResource(instance.database()).backend();
    this.repository = new PersonRepository(backend);
    this.person = PersonCriteria.person;
    this.generator = new PersonGenerator();
  }

  /**
   * Checks if current mongo cluster has <a href="https://www.mongodb.com/docs/manual/replication/">replication</a> enabled.
   * Change Streams only work with replication enabled.
   */
  private static boolean replicaSetEnabled(MongoInstance instance) {
    BsonDocument command = new BsonDocument("replSetGetStatus", BsonBoolean.TRUE);
    return Flowable.fromPublisher(instance.client().getDatabase("admin").runCommand(command))
            .map(doc -> doc.getDouble("ok") == 1D)
            .onErrorReturnItem(false)
            .blockingSingle();
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
