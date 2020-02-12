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

import java.util.EnumSet;
import java.util.Set;

import org.apache.geode.cache.Cache;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.WithSessionCallback;
import org.immutables.criteria.personmodel.AbstractPersonTest;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GeodeExtension.class)
public class GeodePersonTest extends AbstractPersonTest  {

  private final Backend backend;

  public GeodePersonTest(Cache cache) {
    AutocreateRegion autocreate = new AutocreateRegion(cache);
    backend = WithSessionCallback.wrap(new GeodeBackend(GeodeSetup.of(cache)), autocreate);
  }

  @Override
  protected Set<Feature> features() {
    return EnumSet.of(
            Feature.DELETE, Feature.QUERY, Feature.QUERY_WITH_LIMIT, Feature.ORDER_BY, Feature.QUERY_WITH_PROJECTION,
            Feature.ITERABLE_SIZE, Feature.ITERABLE_CONTAINS
    );
  }

  @Override
  protected Backend backend() {
    return backend;
  }

  @Disabled
  @Override
  public void nested() {
    // nested doesn't work yet in Geode. Need custom PDX serializer
  }

  @Test
  void deleteByKey() {
    PersonGenerator generator = new PersonGenerator();
    repository().insert(generator.next().withId("id1"));
    repository().insert(generator.next().withId("id2"));
    repository().insert(generator.next().withId("id3"));

    repository().delete(person.id.in("bad1", "bad2"));

    // nothing was deleted (bad ids)
    check(repository().findAll()).toList(Person::id).hasContentInAnyOrder("id1", "id2", "id3");

    repository().delete(person.id.is("id1"));
    check(repository().findAll()).toList(Person::id).hasContentInAnyOrder("id2", "id3");

    repository().delete(person.id.in("id1", "id1"));
    check(repository().findAll()).toList(Person::id).hasContentInAnyOrder("id2", "id3");

    repository().delete(person.id.in("id2", "id2", "id1"));
    check(repository().findAll()).toList(Person::id).hasContentInAnyOrder("id3");

    repository().delete(person.id.in("id2", "id2", "id1", "id3"));
    check(repository().findAll()).toList(Person::id).isEmpty();
  }

  @Test
  void deleteByQuery() {
    PersonGenerator generator = new PersonGenerator();
    repository().insert(generator.next().withId("id1").withFullName("A").withAge(10));
    repository().insert(generator.next().withId("id2").withFullName("B").withAge(20));
    repository().insert(generator.next().withId("id3").withFullName("C").withAge(30));

    repository().delete(person.age.atMost(9).fullName.is("A"));
    check(repository().findAll()).toList(Person::id).hasContentInAnyOrder("id1", "id2", "id3");

    // delete id1
    repository().delete(person.age.atMost(10).fullName.is("A"));
    check(repository().findAll()).toList(Person::id).hasContentInAnyOrder("id2", "id3");

    // delete id2
    repository().delete(person.age.between(19, 21));
    check(repository().findAll()).toList(Person::id).hasContentInAnyOrder("id3");

    // delete id3
    repository().delete(person.fullName.is("C"));
    check(repository().findAll()).toList(Person::id).isEmpty();
  }

}
