/*
 * Copyright 2020 Immutables Authors and Contributors
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

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.KeyExtractor;
import org.immutables.criteria.backend.WithSessionCallback;
import org.immutables.criteria.personmodel.ImmutablePerson;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.immutables.criteria.personmodel.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.function.UnaryOperator;

import static org.immutables.check.Checkers.check;

/**
 * Check select / insert / delete for a generic key extractor based on lambda function
 *
 * @see KeyExtractor#generic(UnaryOperator)
 */
@ExtendWith(GeodeExtension.class)
class GenericKeyTest {

  private final PersonCriteria person = PersonCriteria.person;

  private final Region<String, Person> region;
  private final PersonRepository repository;
  private final PersonGenerator generator;

  GenericKeyTest(Cache cache) {
    ImmutableGeodeSetup setup = GeodeSetup.builder()
            .regionResolver(RegionResolver.defaultResolver(cache))
            .keyExtractorFactory(genericExtractor())
            .build();
    AutocreateRegion autocreate = new AutocreateRegion(cache);
    Backend backend = WithSessionCallback.wrap(new GeodeBackend(setup), autocreate);
    this.repository = new PersonRepository(backend);
    this.region = cache.getRegion("person");
    this.generator = new PersonGenerator();
  }

  /**
   * Simulate a more complex ID
   */
  private static KeyExtractor.Factory genericExtractor() {
    return KeyExtractor.generic(obj -> {
      Person person = (Person) obj;
      return person.id() + "_" + person.age();
    });
  }

  @Test
  void empty() {
    check(repository.find(person.id.is("1")).fetch()).isEmpty();
    check(repository.find(person.id.in("1", "2")).fetch()).isEmpty();
    check(repository.find(person.id.notIn("1", "2")).fetch()).isEmpty();
  }

  @Test
  void insert() {
    repository.insert(generator.next().withId("id1").withAge(11));
    check(region.keySet()).isOf("id1_11");
    check(region.get("id1_11").id()).is("id1");

    repository.insert(generator.next().withId("id1").withAge(22));
    check(region.keySet()).hasAll("id1_11", "id1_22");
    check(region.get("id1_22").id()).is("id1");

    repository.insert(generator.next().withId("id2").withAge(11));
    check(region.keySet()).hasAll("id1_11", "id1_22", "id2_11");
    check(region.get("id2_11").id()).is("id2");
  }

  @Test
  void select() {
    ImmutablePerson p1 = generator.next().withId("id1").withAge(11);
    repository.insert(p1);
    // see extractor() defined as $id_$age
    check(region.keySet()).isOf("id1_11");
    // was inserted with different id
    check(region.get("id1_11").id()).is("id1");
    check(region.get("id1_11")).is(p1);

    check(repository.find(person.id.is("id1")).fetch()).hasSize(1);
    check(repository.find(person.id.in("id1", "id2")).fetch()).hasSize(1);
    check(repository.find(person.id.is("id1_11")).fetch()).isEmpty();
    check(repository.find(person.id.is("id2")).fetch()).isEmpty();
    check(repository.find(person.id.isNot("id1")).fetch()).isEmpty();
  }

  @Test
  void delete() {
    repository.insert(generator.next().withId("id1").withAge(11));
    repository.insert(generator.next().withId("id2").withAge(22));
    check(region.keySet()).isOf("id1_11", "id2_22");

    // person.id is still 'id1' but key in region is id1_11
    repository.delete(person.id.is("id1_11")); // shouldn't delete anything
    check(region.keySet()).isOf("id1_11", "id2_22");

    repository.delete(person.id.is("id1"));
    check(region.keySet()).isOf("id2_22");

    repository.delete(person.id.is("id2"));
    check(region.keySet()).isEmpty();
  }
}
