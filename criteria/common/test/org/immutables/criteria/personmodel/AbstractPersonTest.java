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

package org.immutables.criteria.personmodel;

import com.google.common.collect.Ordering;
import org.immutables.check.Checkers;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.personmodel.Toy.ToyType;
import org.immutables.criteria.repository.Fetcher;
import org.immutables.criteria.repository.Reader;
import org.immutables.criteria.repository.sync.SyncFetcher;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Set of predefined tests which run for all backends
 */
public abstract class AbstractPersonTest {

  /**
   * Specific feature supported (or not) by a backend
   */
  protected enum Feature {
    QUERY,
    QUERY_WITH_LIMIT,
    QUERY_WITH_OFFSET,
    QUERY_WITH_PROJECTION, // projections are supported
    DELETE,
    DELETE_BY_QUERY,
    WATCH,
    ORDER_BY,
    REGEX, // regular expression for match() operator
    // TODO re-use Operator interface as feature flag
    STRING_EMPTY, // supports empty/non-empty string or null values
    STRING_PREFIX_SUFFIX, // startsWith / endsWith operators are supported
    ITERABLE_SIZE, // supports filtering on iterables sizes
    ITERABLE_CONTAINS, // can search inside inner collections
    ITERABLE_ANY, // can search inside a nested inner collections
    STRING_LENGTH
  }

  protected final PersonCriteria person = PersonCriteria.person;

  /**
   * List of features to be tested
   */
  protected abstract Set<Feature> features();

  /**
   * Backend instantiated by subclasses
   */
  protected abstract Backend backend();

  protected PersonRepository repository;

  protected PersonRepository repository() {
    if (repository == null) {
      Backend backend = Objects.requireNonNull(backend(), "backend is null");
      repository = new PersonRepository(backend);
    }
    return repository;
  }

  /**
   * limit and offset
   */
  @Test
  public void limit() {
    assumeFeature(Feature.QUERY_WITH_LIMIT);
    final int size = 5;
    repository().insertAll(new PersonGenerator().stream()
            .limit(size).collect(Collectors.toList()));

    for (int i = 1; i < size * size; i++) {
      check(repository().findAll().limit(i)).hasSize(Math.min(i, size));
    }

    for (int i = 1; i < 3; i++) {
      check(repository().find(person.id.is("id0")).limit(i)).hasSize(1);
    }

    assumeFeature(Feature.QUERY_WITH_OFFSET);
    check(repository().findAll().limit(1).offset(1)).hasSize(1);
    check(repository().findAll().limit(2).offset(2)).hasSize(2);
    check(repository().findAll().limit(1).offset(size + 1)).empty();
  }

  @Test
  public void comparison() {
   assumeFeature(Feature.QUERY);
   final Person john = new PersonGenerator().next()
            .withId("id123")
            .withDateOfBirth(LocalDate.of(1990, 2, 2))
            .withAge(22);

    insert(john);

    check(person.age.atLeast(22)).hasSize(1);
    check(person.age.greaterThan(22)).empty();
    check(person.age.lessThan(22)).empty();
    check(person.age.atMost(22)).hasSize(1);

    // look up using id
    check(person.id.is("id123")).hasSize(1);
    check(person.id.in("foo", "bar", "id123")).hasSize(1);
    check(person.id.in("foo", "bar", "qux")).empty();

    // jsr310. dates and time
    check(person.dateOfBirth.greaterThan(LocalDate.of(1990, 1, 1))).hasSize(1);
    check(person.dateOfBirth.greaterThan(LocalDate.of(2000, 1, 1))).empty();
    check(person.dateOfBirth.atMost(LocalDate.of(1990, 2, 2))).hasSize(1);
    check(person.dateOfBirth.atMost(LocalDate.of(1990, 2, 1))).empty();
    check(person.dateOfBirth.is(LocalDate.of(1990, 2, 2))).hasSize(1);
  }

  @Test
  public void intComparison() throws Exception {
    Person john = new PersonGenerator().next().withId("john").withFullName("John").withAge(30);
    insert(john);

    check(person.age.is(30)).hasSize(1);
    check(person.age.is(31)).empty();
    check(person.age.isNot(31)).hasSize(1);

    // at least
    check(person.age.atLeast(29)).hasSize(1);
    check(person.age.atLeast(30)).hasSize(1);
    check(person.age.atLeast(31)).empty();

    // at most
    check(person.age.atMost(31)).hasSize(1);
    check(person.age.atMost(30)).hasSize(1);
    check(person.age.atMost(29)).empty();

    check(person.age.greaterThan(29)).hasSize(1);
    check(person.age.greaterThan(30)).empty();
    check(person.age.greaterThan(31)).empty();

    check(person.age.in(Arrays.asList(1, 2, 3))).empty();
    check(person.age.in(1, 2, 3)).empty();
    check(person.age.in(29, 30, 31)).hasSize(1);
    check(person.age.in(Arrays.asList(29, 30, 31))).hasSize(1);
    check(person.age.notIn(1, 2, 3)).hasSize(1);
    check(person.age.notIn(39, 30, 31)).empty();

    check(person.age.atLeast(30).age.atMost(31)).hasSize(1);
    check(person.age.lessThan(30).age.greaterThan(31)).empty();

    // multiple filters on the same field
    check(person.age.is(30).age.greaterThan(31)).empty();
    check(person.age.is(30).age.isNot(30).or().age.is(30)).hasSize(1);
    check(person.age.is(30).age.greaterThan(30).or().age.is(31)).empty();

    // add second person
    Person adam = new PersonGenerator().next().withId("adam").withFullName("Adam").withAge(40);
    insert(adam);

    check(person.age.is(30)).toList().hasContentInAnyOrder(john);
    check(person.age.is(40)).toList().hasContentInAnyOrder(adam);
    check(person.age.atLeast(29)).toList().hasContentInAnyOrder(john, adam);
    check(person.age.atLeast(30)).toList().hasContentInAnyOrder(john, adam);
    check(person.age.atLeast(31)).toList().hasContentInAnyOrder(adam);
    check(person.age.atMost(31)).toList().hasContentInAnyOrder(john);
    check(person.age.atMost(30)).toList().hasContentInAnyOrder(john);
    check(person.age.atMost(29)).empty();
    check(person.age.greaterThan(29)).toList().hasContentInAnyOrder(john, adam);
    check(person.age.greaterThan(30)).toList().hasContentInAnyOrder(adam);
    check(person.age.greaterThan(31)).toList().hasContentInAnyOrder(adam);
    check(person.age.in(Arrays.asList(1, 2, 3))).empty();
    check(person.age.in(Arrays.asList(29, 30, 40, 44))).toList().hasContentInAnyOrder(john, adam);
    check(person.age.notIn(30, 31)).toList().hasContentInAnyOrder(adam);
    check(person.age.notIn(1, 2)).toList().hasContentInAnyOrder(john, adam);
    check(person.age.lessThan(1)).empty();
    check(person.age.lessThan(30)).empty();
    check(person.age.lessThan(31)).hasSize(1);
  }

  @Test
  public void between() {
    Person john = new PersonGenerator().next().withId("john").withFullName("John").withAge(30);
    insert(john);

    check(person.age.between(0, 40)).hasSize(1);
    check(person.age.between(30, 30)).hasSize(1);
    check(person.age.between(30, 31)).hasSize(1);
    check(person.age.between(29, 30)).hasSize(1);
    check(person.age.between(29, 31)).hasSize(1);
    check(person.age.between(30, 35)).hasSize(1);

    check(person.age.between(21, 29)).empty();
    check(person.age.between(29, 29)).empty();
    check(person.age.between(31, 31)).empty();
    check(person.age.between(31, 32)).empty();

    // invalid
    check(person.age.between(31, 30)).empty();
    check(person.age.between(32, 29)).empty();
  }

  /**
   * some variations of boolean logic with {@code NOT} operator
   */
  @Test
  public void notLogic() {
    Person john = new PersonGenerator().next().withId("john").withFullName("John").withAge(30);
    insert(john);

    check(person.not(p -> p.id.is("john"))).empty();
    check(person.not(p -> p.id.is("john").age.is(30))).empty();
    check(person.not(p -> p.id.is("john").age.is(31))).hasSize(1);
    check(person.not(p -> p.id.in("john", "john").age.in(30, 30))).empty();
    check(person.not(p -> p.id.in("john", "john").or().age.in(30, 30))).empty();
    check(person.not(p -> p.id.in("d", "d").age.in(99, 99))).hasSize(1);
    check(person.not(p -> p.id.in("d", "d").or().age.in(99, 99))).hasSize(1);

    check(person.not(p -> p.id.is("john1").or().id.is("john"))).empty();
    check(person.not(p -> p.id.is("john1").or().id.is("john2"))).hasSize(1);
    check(person.not(p -> p.id.isNot("john"))).hasSize(1);
    check(person.not(p -> p.age.atLeast(29).age.atMost(31))).empty();

    check(person.not(p -> p.age.is(30)).not(p2 -> p2.id.is("john"))).empty();
    check(person.not(p -> p.age.is(31)).not(p2 -> p2.id.is("DUMMY"))).hasSize(1);

    // double not
    check(person.not(p -> p.not(p2 -> p2.id.is("john")))).hasSize(1);

    // triple not
    check(person.not(p1 -> p1.not(p2 -> p2.not(p3 -> p3.id.is("john"))))).empty();
    check(person.not(p1 -> p1.not(p2 -> p2.not(p3 -> p3.id.isNot("john"))))).hasSize(1);

    check(person.not(p -> p.age.greaterThan(29))).empty();
    check(person.not(p -> p.age.greaterThan(31))).hasSize(1);

  }

  @Test
  public void basic() {
    assumeFeature(Feature.QUERY);

    final Person john = new PersonGenerator().next()
            .withFullName("John")
            .withIsActive(true)
            .withAge(22);

    insert(john);

    check(person.fullName.is("John")).hasSize(1);
    check(person.fullName.isNot("John")).empty();
    check(person.fullName.is("John")
            .age.isNot(1)).hasSize(1);
    check(person.fullName.is("John")
            .age.is(22)).hasSize(1);
    check(person.fullName.is("_MISSING_")).empty();
    check(person.fullName.in(Collections.emptyList())).empty();
    check(person.fullName.in(Collections.singleton("John"))).hasSize(1);
    check(person.fullName.in("John", "test2")).hasSize(1);
    check(person.fullName.notIn(Collections.singleton("John"))).empty();
    check(person.fullName.notIn("John", "test2")).empty();

    // true / false
    check(person.isActive.isTrue()).hasSize(1);
    check(person.isActive.isFalse()).empty();

    // isPresent / isAbsent
    check(person.address.isAbsent()).empty();
    check(person.address.isPresent()).notEmpty();

    // simple OR
    check(person.address.isAbsent().or().address.isPresent()).notEmpty();
    check(person.isActive.isFalse().or().isActive.isTrue()).notEmpty();
  }

  /**
   * Basic queries on ID
   */
  @Test
  void id() {
    assumeFeature(Feature.QUERY);
    PersonGenerator generator = new PersonGenerator();

    insert(generator.next().withId("id1"));
    insert(generator.next().withId("id2"));

    check(person.id.is("id1")).toList(Person::id).isOf("id1");
    check(person.id.is("id2")).toList(Person::id).isOf("id2");

    check(person.id.in("id1", "id2")).toList(Person::id).hasContentInAnyOrder("id1", "id2");
    check(person.id.in(Collections.singleton("id1"))).toList(Person::id).isOf("id1");

    // negatives
    check(person.id.isNot("id1")).toList(Person::id).hasContentInAnyOrder("id2");
    check(person.id.isNot("id2")).toList(Person::id).hasContentInAnyOrder("id1");

    check(person.id.notIn(Collections.singleton("id1"))).toList(Person::id).hasContentInAnyOrder("id2");
    check(person.id.notIn(Collections.singleton("id2"))).toList(Person::id).hasContentInAnyOrder("id1");
  }

  @Test
  public void nested() {
    final ImmutablePerson john = new PersonGenerator().next().withBestFriend(ImmutableFriend.builder().hobby("ski").build());
    insert(john);
    final Address address = john.address().get();
    check(person.address.value().city.is(address.city())).notEmpty();
    check(person.address.value().zip.is(address.zip())).notEmpty();
    check(person.address.value().zip.is(address.zip())).toList().hasContentInAnyOrder(john);
    check(person.address.value().state.is(address.state())).toList().hasContentInAnyOrder(john);
    check(person.address.value().city.is("__MISSING__")).empty();
    check(person.bestFriend.value().hobby.is("ski")).notEmpty();
    check(person.bestFriend.value().hobby.is("__MISSING__")).empty();
  }

  @Test
  public void empty() {
    check(repository().findAll()).empty();
    check(repository().find(person)).empty();

    insert(new PersonGenerator().next());
    check(repository().findAll()).notEmpty();
    check(repository().find(person)).notEmpty();
  }

  @Test
  public void orderBy() {
    assumeFeature(Feature.ORDER_BY);
    final PersonGenerator generator = new PersonGenerator();
    final int count = 10;
    final List<Person> persons = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      persons.add(generator.next().withAge(i).withFullName("name" + (count - i - 1)).withId("id" + i));
    }
    // to ensure result is sorted (not by insertion order)
    Collections.shuffle(persons);
    insert(persons);

    check(repository().findAll().orderBy(person.age.asc())).hasSize(count);
    check(repository().findAll().orderBy(person.age.asc()).limit(1)).toList(Person::fullName).isOf("name9");
    check(repository().findAll().orderBy(person.age.asc()).limit(2)).toList(Person::fullName).isOf("name9", "name8");
    check(repository().findAll().orderBy(person.age.asc()).limit(3)).toList(Person::fullName).isOf("name9", "name8", "name7");

    check(repository().findAll().orderBy(person.fullName.asc()).limit(1)).toList(Person::fullName).isOf("name0");
    check(repository().findAll().orderBy(person.fullName.asc()).limit(2)).toList(Person::fullName).isOf("name0", "name1");
    check(repository().findAll().orderBy(person.fullName.asc()).limit(3)).toList(Person::fullName).isOf("name0", "name1", "name2");

    check(repository().findAll().orderBy(person.age.desc())).hasSize(count);
    check(repository().findAll().orderBy(person.age.desc()).limit(1)).toList(Person::fullName).isOf("name0");
    check(repository().findAll().orderBy(person.age.desc()).limit(2)).toList(Person::fullName).isOf("name0", "name1");
    check(repository().findAll().orderBy(person.age.desc()).limit(3)).toList(Person::fullName).isOf("name0", "name1", "name2");

    check(repository().findAll().orderBy(person.fullName.desc())).hasSize(count);
    check(repository().findAll().orderBy(person.fullName.desc()).limit(1)).toList(Person::fullName).isOf("name9");
    check(repository().findAll().orderBy(person.fullName.desc()).limit(2)).toList(Person::fullName).isOf("name9", "name8");
    check(repository().findAll().orderBy(person.fullName.desc()).limit(3)).toList(Person::fullName).isOf("name9", "name8", "name7");


    check(repository().findAll().orderBy(person.fullName.asc(), person.age.asc()).limit(1)).toList(Person::fullName).isOf("name0");
    check(repository().findAll().orderBy(person.fullName.asc(), person.age.asc()).limit(2)).toList(Person::fullName).isOf("name0", "name1");
    check(repository().findAll().orderBy(person.fullName.desc(), person.age.asc()).limit(2)).toList(Person::fullName).isOf("name9", "name8");
    check(repository().findAll().orderBy(person.age.desc(), person.fullName.desc()).limit(2)).toList(Person::fullName).isOf("name0", "name1");


    assertOrdered(Person::age, repository().findAll().orderBy(person.age.asc()), Ordering.natural());
    assertOrdered(Person::age, repository().findAll().orderBy(person.age.asc()).limit(5), Ordering.natural());
    assertOrdered(Person::age, repository().findAll().orderBy(person.age.desc()), Ordering.natural().reverse());
    assertOrdered(Person::age, repository().findAll().orderBy(person.age.desc()).limit(5), Ordering.natural().reverse());
  }

  @Test
  public void regex() {
    assumeFeature(Feature.REGEX);
    final PersonGenerator generator = new PersonGenerator();
    insert(generator.next().withFullName("John"));

    check(repository().find(person.fullName.matches(Pattern.compile("John")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile("J.*n")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile("J..n")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile("J...")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile("...n")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile("^Jo")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile("hn$")))).hasSize(1);
    check(repository().find(person.fullName.not(s ->s.matches(Pattern.compile("J.*n"))))).empty();
    check(repository().find(person.fullName.matches(Pattern.compile("J\\w+n")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile(".*")))).hasSize(1);

    insert(generator.next().withFullName("Mary"));
    check(repository().find(person.fullName.matches(Pattern.compile("J.*n")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile("M.*ry")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile("^Ma")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile("ry$")))).hasSize(1);
    check(repository().find(person.fullName.matches(Pattern.compile(".*")))).hasSize(2);
  }

  @Test
  public void startsOrEndsWith() {
    assumeFeature(Feature.STRING_PREFIX_SUFFIX);
    final PersonGenerator generator = new PersonGenerator();
    insert(generator.next().withFullName("John"));

    // == starts with
    check(repository().find(person.fullName.startsWith(""))).hasSize(1);
    check(repository().find(person.fullName.startsWith("J"))).hasSize(1);
    check(repository().find(person.fullName.startsWith("Jo"))).hasSize(1);
    check(repository().find(person.fullName.startsWith("Jooo"))).empty();
    check(repository().find(person.fullName.startsWith("John"))).hasSize(1);
    check(repository().find(person.fullName.startsWith("j"))).empty();
    check(repository().find(person.fullName.startsWith("john"))).empty(); // not case sensitive

    // === ends with
    check(repository().find(person.fullName.endsWith(""))).hasSize(1);
    check(repository().find(person.fullName.endsWith("n"))).hasSize(1);
    check(repository().find(person.fullName.endsWith("hn"))).hasSize(1);
    check(repository().find(person.fullName.endsWith("ohn"))).hasSize(1);
    check(repository().find(person.fullName.endsWith("N"))).empty();

    insert(generator.next().withFullName("Mary"));
    check(repository().find(person.fullName.startsWith(""))).hasSize(2);
    check(repository().find(person.fullName.startsWith("Ma"))).hasSize(1);
    check(repository().find(person.fullName.startsWith("Mary"))).hasSize(1);
    check(repository().find(person.fullName.startsWith("Jo"))).hasSize(1);

    check(repository().find(person.fullName.endsWith(""))).hasSize(2);
    check(repository().find(person.fullName.endsWith("y"))).hasSize(1);
    check(repository().find(person.fullName.endsWith("Mary"))).hasSize(1);
  }

  @Test
  public void stringContains() {
    assumeFeature(Feature.STRING_PREFIX_SUFFIX);
    final PersonGenerator generator = new PersonGenerator();
    // on empty
    check(repository().find(person.fullName.contains(""))).empty();
    check(repository().find(person.fullName.contains("J"))).empty();
    check(repository().find(person.fullName.contains("John"))).empty();

    insert(generator.next().withFullName("John"));

//    check(repository().find(person.fullName.contains(""))).hasSize(1);
    check(repository().find(person.fullName.contains("J"))).hasSize(1);
    check(repository().find(person.fullName.contains("John"))).hasSize(1);
    check(repository().find(person.fullName.contains("John1"))).empty();
    check(repository().find(person.fullName.contains("Mary"))).empty();
    check(repository().find(person.fullName.contains("X"))).empty();
    check(repository().find(person.fullName.contains("oh"))).hasSize(1);
    check(repository().find(person.fullName.contains("ohn"))).hasSize(1);
    check(repository().find(person.fullName.contains("n"))).hasSize(1);
  }

  @Test
  public void iterableHasSize() {
    assumeFeature(Feature.ITERABLE_SIZE);
    final PersonGenerator generator = new PersonGenerator();
    // no pets
    insert(generator.next().withFullName("John"));
    // empty pets
    insert(generator.next().withFullName("Mary").withPets(Collections.emptyList()));
    // single pet
    insert(generator.next().withFullName("Adam").withPets(ImmutablePet.builder().name("fluffy").type(Pet.PetType.gecko).build()));
    insert(generator.next().withFullName("Paul").withPets(ImmutablePet.builder().name("nummy").type(Pet.PetType.cat).build()));

    // two pets
    insert(generator.next().withFullName("Emma").withPets(
            ImmutablePet.builder().name("fluffy").type(Pet.PetType.gecko).build(),
            ImmutablePet.builder().name("oopsy").type(Pet.PetType.panda).build()
    ));

    //TODO handle edge cases. empty vs missing iterable:
    // 1. how is it persisted vs in-memory representation ?
    // 2. size == 0 vs empty / notEmpty

    // check(repository().find(person.pets.hasSize(0))).toList(Person::fullName).isOf("Mary", "John");
    check(repository().find(person.pets.hasSize(1))).toList(Person::fullName).hasContentInAnyOrder("Adam", "Paul");
    check(repository().find(person.pets.hasSize(2))).toList(Person::fullName).isOf("Emma");

    // negation
    check(repository().find(person.not(p -> p.pets.hasSize(1)))).toList(Person::fullName).not().isOf("Adam", "Paul");
    check(repository().find(person.not(p -> p.pets.hasSize(2)))).toList(Person::fullName).not().isOf("Emma");
  }

  @Test
  public void emptyNotEmptyIterable() {
    assumeFeature(Feature.ITERABLE_SIZE);
    final PersonGenerator generator = new PersonGenerator();
    // no pets
    insert(generator.next().withFullName("John").withInterests());
    insert(generator.next().withFullName("Mary").withInterests("skiing"));
    insert(generator.next().withFullName("Adam").withInterests("skiing", "biking"));

    check(repository().find(person.interests.isEmpty())).toList(Person::fullName).hasContentInAnyOrder("John");
    check(repository().find(person.interests.notEmpty())).toList(Person::fullName).hasContentInAnyOrder("Mary", "Adam");
    check(repository().find(person.not(p -> p.interests.notEmpty()))).toList(Person::fullName).hasContentInAnyOrder("John");
    check(repository().find(person.not(p -> p.interests.isEmpty()))).toList(Person::fullName).hasContentInAnyOrder("Mary", "Adam");
  }

  @Test
  public void iterableContains() {
    assumeFeature(Feature.ITERABLE_CONTAINS);
    final PersonGenerator generator = new PersonGenerator();
    insert(generator.next().withFullName("John"));
    insert(generator.next().withFullName("Mary").withInterests("skiing"));
    insert(generator.next().withFullName("Adam").withInterests("hiking", "swimming"));
    insert(generator.next().withFullName("Emma").withInterests("cooking", "skiing"));

    check(repository().find(person.interests.contains("skiing"))).toList(Person::fullName).hasContentInAnyOrder("Mary", "Emma");
    check(repository().find(person.interests.contains("hiking"))).toList(Person::fullName).isOf("Adam");
    check(repository().find(person.interests.contains("cooking"))).toList(Person::fullName).isOf("Emma");
    check(repository().find(person.interests.contains("swimming"))).toList(Person::fullName).isOf("Adam");
    check(repository().find(person.interests.contains("dancing"))).empty();
  }
  
  @Test
  public void iterableAny() {
    this.assumeFeature(Feature.ITERABLE_ANY);
    final PersonGenerator generator = new PersonGenerator();
    // no pets
    this.insert(generator.next().withFullName("John"));
    // empty pets
    this.insert(generator.next().withFullName("Mary").withPets(Collections.emptyList()));
    // single pet without toys
    this.insert(
        generator.next().withFullName("Paul")
            .withPets(
                ImmutablePet.builder()
                    .name("nummy")
                    .type(Pet.PetType.cat)
                    .build()));
    // single pet with address and one toy
    this.insert(
        generator.next().withFullName("Christine")
            .withPets(
                ImmutablePet.builder()
                    .name("gecko")
                    .type(Pet.PetType.gecko)
                    .address(
                        ImmutableAddress.builder()
                            .street("209 M St NE")
                            .city("Washington, D.C.")
                            .state(Address.State.DC)
                            .zip("20002")
                            .build())
                    .toys(
                        Arrays.asList(
                            ImmutableToy.builder()
                                .name("circle")
                                .type(ToyType.ring)
                                .build()))
                    .build()));
    // single pet with address and two toy
    this.insert(
        generator.next().withFullName("Adam")
            .withPets(
                ImmutablePet.builder()
                    .name("fluffy")
                    .type(Pet.PetType.gecko)
                    .address(
                        ImmutableAddress.builder()
                            .street("Park Avenue 345")
                            .city("New York")
                            .state(Address.State.NY)
                            .zip("10154")
                            .build())
                    .toys(
                        Arrays.asList(
                            ImmutableToy.builder()
                                .name("ringy")
                                .type(ToyType.ring)
                                .build(),
                            ImmutableToy.builder()
                                .name("roby")
                                .type(ToyType.robot)
                                .build()))
                    .build()));
    // two pets one with a toy
    this.insert(
        generator.next().withFullName("Emma").withPets(
            ImmutablePet.builder().name("fluffy").type(Pet.PetType.dog).build(),
            ImmutablePet.builder().name("oopsy").type(Pet.PetType.panda)
                .address(
                    ImmutableAddress.builder()
                        .street("Park Avenue 277")
                        .city("New York")
                        .state(Address.State.NY)
                        .zip("10172")
                        .build())
                .toys(
                    Arrays.asList(
                        ImmutableToy.builder()
                            .name("roby")
                            .type(ToyType.robot)
                            .build()))
                .build()));
    
    // find element in inner-collections
    this.check(this.repository().find(this.person.pets.isEmpty())).toList(Person::fullName).hasContentInAnyOrder("John", "Mary");
    // string handling
    this.check(this.repository().find(this.person.pets.any().name.is("daisy"))).empty();
    this.check(this.repository().find(this.person.pets.any().name.startsWith("oo"))).toList(Person::fullName).isOf("Emma");
    this.check(this.repository().find(this.person.pets.any().name.contains("fluffy"))).toList(Person::fullName).hasContentInAnyOrder("Adam", "Emma");
    this.check(this.repository().find(this.person.pets.any().name.endsWith("y"))).toList(Person::fullName).hasContentInAnyOrder("Adam", "Paul", "Emma");
    this.check(this.repository().find(this.person.pets.any().name.in("gecko", "oopsy"))).toList(Person::fullName).hasContentInAnyOrder("Christine", "Emma");
    this.check(this.repository().find(this.person.pets.any().name.notIn("gecko", "oopsy"))).toList(Person::fullName).hasContentInAnyOrder("Adam", "Paul", "Emma");
    // Optional handling
    this.check(this.repository().find(this.person.pets.any().address.value().city.startsWith("New"))).toList(Person::fullName).hasContentInAnyOrder("Adam", "Emma");
    this.check(this.repository().find(this.person.pets.any().address.value().city.startsWith("New").and(this.person.pets.any().toys.any().type.is(ToyType.ring)))).toList(Person::fullName).isOf("Adam");
    this.check(this.repository().find(this.person.pets.any().address.value().zip.endsWith("72").or(this.person.pets.any().toys.any().type.is(ToyType.ring)))).toList(Person::fullName).hasContentInAnyOrder("Adam", "Emma", "Christine");
    // nested collection handling
    this.check(this.repository().find(this.person.pets.any().toys.isEmpty())).toList(Person::fullName).hasContentInAnyOrder("Paul", "Emma");
    this.check(this.repository().find(this.person.pets.any().toys.any().name.endsWith("y"))).toList(Person::fullName).hasContentInAnyOrder("Adam", "Emma");
    this.check(this.repository().find(this.person.pets.any().toys.any().name.in("ringy", "roby"))).toList(Person::fullName).hasContentInAnyOrder("Adam", "Emma");
    this.check(this.repository().find(this.person.pets.any().toys.any().name.notIn("ringy", "roby"))).toList(Person::fullName).hasContentInAnyOrder("Christine");
    this.check(this.repository().find(this.person.pets.any().toys.any().type.is(ToyType.ring))).toList(Person::fullName).hasContentInAnyOrder("Adam", "Christine");
    this.check(this.repository().find(this.person.pets.any().toys.any().type.is(ToyType.robot).and(this.person.pets.any().address.value().zip.is("10154")))).toList(Person::fullName).isOf("Adam");
    this.check(this.repository().find(this.person.pets.any().toys.any().type.is(ToyType.ring).or(this.person.pets.any().address.value().zip.endsWith("72")))).toList(Person::fullName).hasContentInAnyOrder("Adam", "Emma", "Christine");
    this.check(this.repository().find(this.person.pets.any().toys.any().not(p -> p.type.is(ToyType.ring)))).toList(Person::fullName).hasContentInAnyOrder("Adam", "Emma");
    // nested collection with element matching multiple conditions
    this.check(this.repository().find(this.person.pets.any().with(pet -> pet.name.is("fluffy").and(pet.type.is(Pet.PetType.gecko))))).toList(Person::fullName).isOf("Adam");
    this.check(this.repository().find(this.person.pets.any().with(pet -> pet.name.is("fluffy").and(pet.type.not(type -> type.is(Pet.PetType.gecko)))))).toList(Person::fullName).hasContentInAnyOrder("Emma");
  }

  @Test
  public void stringLength() {
    assumeFeature(Feature.STRING_LENGTH);
    final PersonGenerator generator = new PersonGenerator();
    insert(generator.next().withFullName("John").withNickName(Optional.empty()));
    insert(generator.next().withFullName("Adam").withNickName(""));
    insert(generator.next().withFullName("Mary").withNickName("a"));
    insert(generator.next().withFullName("Emma").withNickName("bb"));
    insert(generator.next().withFullName("James").withNickName("ccc"));

    check(repository().find(person.nickName.hasLength(0))).toList(Person::fullName).hasContentInAnyOrder("Adam");
    check(repository().find(person.nickName.hasLength(1))).toList(Person::fullName).hasContentInAnyOrder("Mary");
    check(repository().find(person.nickName.hasLength(2))).toList(Person::fullName).hasContentInAnyOrder("Emma");
    check(repository().find(person.nickName.hasLength(3))).toList(Person::fullName).hasContentInAnyOrder("James");
    check(repository().find(person.nickName.hasLength(4))).toList(Person::fullName).isEmpty();
  }

  /**
   * Edge case for empty/non-empty string for null values.
   * {@code string.notEmpty} should not return empty / missing values
   */
  @Test
  public void stringEmptyNotEmpty() {
    assumeFeature(Feature.STRING_EMPTY);
    final PersonGenerator generator = new PersonGenerator();
    insert(generator.next().withFullName("John").withNickName(Optional.empty()));
    insert(generator.next().withFullName("Adam").withNickName(""));
    insert(generator.next().withFullName("Mary").withNickName("a"));

    check(repository().find(person.nickName.isEmpty())).toList(Person::fullName).hasContentInAnyOrder("Adam");
    check(repository().find(person.nickName.notEmpty())).toList(Person::fullName).hasContentInAnyOrder("Mary");
    check(repository().find(person.nickName.isAbsent())).toList(Person::fullName).hasContentInAnyOrder("John");
    check(repository().find(person.nickName.isPresent())).toList(Person::fullName).hasContentInAnyOrder("Adam", "Mary");
  }

  @Test
  public void projection_basic() {
    assumeFeature(Feature.QUERY_WITH_PROJECTION);
    final PersonGenerator generator = new PersonGenerator();
    final LocalDate dob = LocalDate.now().minusYears(22);
    insert(generator.next().withId("id1").withFullName("John").withNickName(Optional.empty()).withAge(21).withIsActive(true).withDateOfBirth(dob));
    insert(generator.next().withId("id2").withFullName("Mary").withNickName("a").withAge(22).withIsActive(false).withDateOfBirth(dob));
    insert(generator.next().withId("id3").withFullName("Emma").withNickName("b").withAge(23).withIsActive(true).withDateOfBirth(dob));

    Checkers.check(repository().findAll().select(person.age).fetch()).hasContentInAnyOrder(21, 22, 23);
    Checkers.check(repository().findAll().select(person.fullName).fetch()).hasContentInAnyOrder("John", "Mary", "Emma");
    Checkers.check(repository().findAll().select(person.id).fetch()).hasContentInAnyOrder("id1", "id2", "id3");
    Checkers.check(repository().findAll().select(person.dateOfBirth).fetch()).hasContentInAnyOrder(dob, dob, dob);
    Checkers.check(repository().findAll().select(person.isActive).fetch()).hasContentInAnyOrder(true, false, true);
    Checkers.check(repository().findAll().select(person.isActive, person.dateOfBirth).map((x, date) -> date).fetch()).hasContentInAnyOrder(dob, dob, dob);

    Checkers.check(repository().findAll().select(person.id, person.fullName).map((k, v) -> new AbstractMap.SimpleImmutableEntry<>(k, v)).fetch())
            .hasContentInAnyOrder(new AbstractMap.SimpleImmutableEntry<>("id1", "John"), new AbstractMap.SimpleImmutableEntry<>("id2", "Mary"), new AbstractMap.SimpleImmutableEntry<>("id3", "Emma"));
  }

  @Test
  public void projection_tuple() {
    assumeFeature(Feature.QUERY_WITH_PROJECTION);
    final PersonGenerator generator = new PersonGenerator();
    final LocalDate dob = LocalDate.now().minusYears(22);
    insert(generator.next().withId("id1").withFullName("John").withNickName(Optional.empty()).withAge(21).withIsActive(true).withDateOfBirth(dob));
    insert(generator.next().withId("id2").withFullName("Mary").withNickName("a").withAge(22).withIsActive(false).withDateOfBirth(dob));
    insert(generator.next().withId("id3").withFullName("Emma").withNickName("b").withAge(23).withIsActive(true).withDateOfBirth(dob));

    Checkers.check(repository().findAll().select(Collections.singleton(person.age)).map(x -> x.get(person.age)).fetch()).hasContentInAnyOrder(21, 22, 23);
    Checkers.check(repository().findAll().select(Collections.singleton(person.fullName)).map(x -> x.get(person.fullName)).fetch()).hasContentInAnyOrder("John", "Mary", "Emma");
    Checkers.check(repository().findAll().select(Collections.singleton(person.dateOfBirth)).map(x -> x.get(person.dateOfBirth)).fetch()).hasContentInAnyOrder(dob, dob, dob);
    Checkers.check(repository().findAll().select(Arrays.asList(person.isActive, person.dateOfBirth)).map(x -> x.get(person.dateOfBirth)).fetch()).hasContentInAnyOrder(dob, dob, dob);
    Checkers.check(repository().findAll().select(person.isActive, person.dateOfBirth).map(tuple -> tuple.get(person.dateOfBirth)).fetch()).hasContentInAnyOrder(dob, dob, dob);
  }

  @Test
  public void projection_nulls() {
    assumeFeature(Feature.QUERY_WITH_PROJECTION);
    final PersonGenerator generator = new PersonGenerator();
    insert(generator.next().withId("id1").withFullName("John").withNickName(Optional.empty()).withAge(21).withBestFriend(Optional.empty()));
    insert(generator.next().withId("id2").withFullName("Mary").withNickName(Optional.empty()).withAge(22).withBestFriend(Optional.empty()));
    insert(generator.next().withId("id3").withFullName("Emma").withNickName(Optional.empty()).withAge(23).withBestFriend(Optional.empty()));

    Checkers.check(repository().findAll().select(person.nickName)
            .fetch())
            .hasContentInAnyOrder(Optional.empty(), Optional.empty(), Optional.empty());

    // with tuple
    Checkers.check(repository().findAll().select(Collections.singleton(person.nickName)).map(x -> x.get(person.nickName))
            .fetch())
            .hasContentInAnyOrder(Optional.empty(), Optional.empty(), Optional.empty());

    // normally bestFriend.hobby is Projection<String> but with person.bestFriend.hobby it becomes Projection<Optional<Sting>> because person.bestFriend is optional
    Checkers.check(repository().findAll().select(person.bestFriend.value().hobby)
            .asOptional()
            .fetch())
            .hasContentInAnyOrder(Optional.empty(), Optional.empty(), Optional.empty());

    // same as before but with tuple
    Checkers.check(repository().findAll().select(Collections.singleton(person.bestFriend.value().hobby))
            .map(x -> Optional.ofNullable(x.get(person.bestFriend.value().hobby)))
            .fetch())
            .hasContentInAnyOrder(Optional.empty(), Optional.empty(), Optional.empty());

    Checkers.check(repository().findAll().select(person.nickName, person.bestFriend.value().hobby)
            .map((nickName, hobby) -> nickName.orElse("") + (hobby == null ? "" : hobby))
            .fetch())
            .hasContentInAnyOrder("", "", "");

    Checkers.check(repository().findAll().select(Arrays.asList(person.nickName, person.bestFriend.value().hobby))
            .map(tuple -> tuple.get(person.nickName).orElse("") + Optional.ofNullable(tuple.get(person.bestFriend.value().hobby)).orElse(""))
            .fetch())
            .hasContentInAnyOrder("", "", "");
  }

  /**
   * Projection of fields which have container-like attributes: {@code Optional<T>}, {@code List<T>} etc.
   */
  @Test
  public void projection_ofContainers() {
    assumeFeature(Feature.QUERY_WITH_PROJECTION);
    final PersonGenerator generator = new PersonGenerator();
    insert(generator.next().withFullName("John").withNickName(Optional.empty()).withInterests("one").withIsActive(true));
    insert(generator.next().withFullName("Mary").withNickName("a").withInterests("one", "two", "three").withIsActive(false));
    insert(generator.next().withFullName("Emma").withNickName("b").withInterests("four").withIsActive(true));

    // nickname
    Checkers.check(repository().findAll().select(person.nickName).fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of("a"), Optional.of("b"));
    Checkers.check(repository().findAll().select(person.fullName, person.nickName).map((k, v) -> k + "-" + v.orElse("")).fetch())
            .hasContentInAnyOrder("John-", "Mary-a", "Emma-b");

    Checkers.check(repository().findAll().select(person.address).fetch()).notEmpty();
    Checkers.check(repository().findAll().select(person.bestFriend).fetch()).notEmpty();
  }

  private void assumeFeature(Feature feature) {
    Assumptions.assumeTrue(features().contains(feature), String.format("Feature %s not supported by current backend", feature));
  }

  private static <T extends Comparable<T>> void assertOrdered(Function<Person, T> extractor, SyncFetcher<Person> reader, Ordering<T> ordering) {
    List<T> parts = reader.fetch().stream().map(extractor).collect(Collectors.toList());
    if (!ordering.isOrdered(parts)) {
      throw new AssertionError(String.format("%s is not ordered. Expected: %s", parts, ordering.sortedCopy(parts)));
    }
  }

  protected void insert(Person ... persons) {
    insert(Arrays.asList(persons));
  }

  protected void insert(Iterable<? extends Person> persons) {
    repository().insertAll(persons);
  }

  protected CriteriaChecker<Person> check(Fetcher<?> reader) {
    return CriteriaChecker.ofReader((Reader<?>) reader);
  }

  private CriteriaChecker<Person> check(Criterion<Person> criterion) {
    return check(repository().find(criterion));
  }

}
