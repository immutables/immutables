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

package org.immutables.criteria.geode;

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;
import org.immutables.criteria.backend.WatchEvent;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.immutables.criteria.personmodel.PersonRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.immutables.check.Checkers.check;

/**
 * Unfortunately CQ can't be tested with an embedded server. For Geode, there can be only
 * one instance of Cache in JVM: server or client. CQ is client / server topology.
 *
 * <p>This test requires an external Geode instance
 *
 * <p>Temporary test until we have a proper way to start real geode server
 *
 * <pre>
 *   copy artifact jars from common/target/*.jar into $GEMFIRE_HOME/extensions/
 *   manually start gfsh
 *   $ start locator --name=locator1 --port=10334
 *   $ start server --name=server1 --server-port=40411
 *   $ create region --name=persons --type=REPLICATE
 * </pre>
 */
@Ignore // ignored because requires external gemfire instance
public class GeodeCqTest {

  private ClientCache clientCache;

  private Region<String, Person> region;

  @Before
  public void setUp() throws Exception {
    this.clientCache = new ClientCacheFactory()
            .addPoolLocator("127.0.0.1", 10334)
            .setPdxSerializer(new ReflectionBasedAutoSerializer(Person.class.getPackage().getName()))
            .setPoolSubscriptionEnabled(true)
            .create();

    this.region = clientCache
            .<String, Person>createClientRegionFactory(ClientRegionShortcut.CACHING_PROXY)
            .create("persons");

    region.clear();
  }

  @After
  public void tearDown() throws Exception {
    if (clientCache != null) {
      clientCache.close();
    }
  }

  @Test
  public void pubsub() throws Exception {

    PersonRepository repository = new PersonRepository(new GeodeBackend(GeodeSetup.of(x -> region)));

    TestSubscriber<WatchEvent<Person>> events = Flowable.fromPublisher(repository.watcher(PersonCriteria.person).watch())
            .test();

    final PersonGenerator generator = new PersonGenerator();
    final int count = 4;
    for (int i = 0; i < count; i++) {
      Flowable.fromPublisher(repository.insert(generator.next().withId("id" + i)))
              .test()
              .awaitDone(1, TimeUnit.SECONDS)
              .assertComplete();
    }

    check(region.keySet()).notEmpty();
    // ensure (de)serialization is successful
    check(region.query("true")).hasSize(count);

    events.awaitCount(count);
    events.assertNoErrors();
    events.assertValueCount(count);
    check(events.values().stream().map(e -> e.newValue().get().id()).collect(Collectors.toList())).hasContentInAnyOrder("id0", "id1", "id2", "id3");
  }
}
