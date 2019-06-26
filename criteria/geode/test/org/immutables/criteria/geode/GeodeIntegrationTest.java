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
import org.immutables.criteria.Criterion;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.immutables.criteria.personmodel.PersonRepository;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    region.clear();
  }

  @Test
  public void basic() {
    insert(generator.next().withId("one"));

    check(region.keySet()).hasContentInAnyOrder("one");

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

    check(findAll()).isEmpty();
  }

  @Test
  public void comparison() {
    insert(generator.next().withId("one").withAge(22));
    check(region.keySet()).hasContentInAnyOrder("one");

    check(find(PersonCriteria.create().age.isAtLeast(22))).hasSize(1);
    check(find(PersonCriteria.create().age.isAtLeast(23).id.isEqualTo("one"))).isEmpty();
  }

  @Test
  public void not() {
    insert(generator.next().withId("one").withAge(22));
    check(find(PersonCriteria.create().age.isNotEqualTo(22))).isEmpty();
  }

  @Test
  public void delete() {
    check(region.values()).isEmpty();
    check(findAll()).isEmpty();


    // delete all
    check(Flowable.fromPublisher(repository.delete(PersonCriteria.create())).blockingFirst()).notNull();
    check(region.keySet()).isEmpty();

    insert(generator.next().withId("test"));

    check(Flowable.fromPublisher(repository.delete(PersonCriteria.create().id.isIn("testBAD", "test")))
            .blockingFirst()).notNull();
    check(region.keySet()).hasSize(0);

    // insert again
    insert(generator.next().withId("test").withNickName("nick123"));

    check(Flowable.fromPublisher(repository.delete(PersonCriteria.create().nickName.value().isEqualTo("nick123")))
            .blockingFirst()).notNull();

    // delete by query doesn't work yet
    // check(region.keySet()).hasSize(0);

  }

  private List<Person> findAll() {
    return find(PersonCriteria.create());
  }

  private List<Person> find(Criterion<Person> criteria) {
    return Flowable.fromPublisher(repository.find(criteria).fetch()).test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertComplete()
            .values();
  }

  private void insert(Person ... persons) {
    List<String> ids = Arrays.stream(persons).map(Person::id).collect(Collectors.toList());

    Flowable.fromPublisher(repository.insert(persons))
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertComplete();

    check(region.keySet()).hasContentInAnyOrder(ids);
  }
}
