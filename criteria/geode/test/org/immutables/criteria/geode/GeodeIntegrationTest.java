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
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.immutables.criteria.personmodel.PersonRepository;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.immutables.check.Checkers.check;

public class GeodeIntegrationTest {

  @ClassRule
  public static final GeodeResource GEODE = GeodeResource.create();

  private static Region<String, Person> region;

  private PersonRepository repository;

  private PersonGenerator generator = new PersonGenerator();

  @BeforeClass
  public static void setup() {
    Cache cache = GEODE.cache();
    region =  cache.<String, Person>createRegionFactory()
            .setKeyConstraint(String.class)
            .setValueConstraint(Person.class)
            .create("mytest");
  }

  @Before
  public void setUp() throws Exception {
    repository = new PersonRepository(new GeodeBackend(region));
  }

  @Test
  public void basic() {
    Flowable.fromPublisher(repository.insert(generator.next().withId("one")))
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertComplete();

    Person person= Flowable.fromPublisher(repository.findById("one").fetch())
            .blockingFirst();

    check(person.id()).is("one");

    region.clear();

    // nothing returned
    Flowable.fromPublisher(repository.findById("one").fetch())
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertComplete()
            .assertValueCount(0);

    Flowable.fromPublisher(repository.findAll().fetch())
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertComplete()
            .assertValueCount(0);
  }
}
