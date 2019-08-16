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
import io.reactivex.Flowable;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.repository.Reader;
import org.immutables.criteria.repository.reactive.ReactiveReader;
import org.junit.Assume;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    DELETE,
    DELETE_BY_QUERY,
    WATCH,
    ORDER_BY,
    REGEX, // regular expression for match() operator
    // TODO re-use Operator interface as feature flag
    STRING_PREFIX_SUFFIX, // startsWith / endsWith operators a are supported
    ITERABLE_SIZE, // supports filtering on iterables sizes
    ITERABLE_CONTAINS, // can search inside inner collections
    STRING_LENGTH
  }

  /**
   * List of features to be tested
   */
  protected abstract Set<Feature> features();

  /**
   * Exposted repository
   */
  protected abstract PersonRepository repository();

  /**
   * Create person criteria
   */
  protected static PersonCriteria criteria() {
    return PersonCriteria.person;
  }

  /**
   * limit and offset
   */
  @Test
  public void limit() {
    assumeFeature(Feature.QUERY_WITH_LIMIT);
    final int size = 5;
    Flowable.fromPublisher(repository().insert(new PersonGenerator().stream()
            .limit(size).collect(Collectors.toList())))
            .singleOrError()
            .blockingGet();

    for (int i = 1; i < size * size; i++) {
      check(repository().findAll().limit(i)).hasSize(Math.min(i, size));
    }

    for (int i = 1; i < 3; i++) {
      check(repository().find(criteria().id.is("id0")).limit(i)).hasSize(1);
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

    check(criteria().age.atLeast(22)).hasSize(1);
    check(criteria().age.greaterThan(22)).empty();
    check(criteria().age.lessThan(22)).empty();
    check(criteria().age.atMost(22)).hasSize(1);

    // look up using id
    check(criteria().id.is("id123")).hasSize(1);
    check(criteria().id.in("foo", "bar", "id123")).hasSize(1);
    check(criteria().id.in("foo", "bar", "qux")).empty();

    // jsr310. dates and time
    check(criteria().dateOfBirth.greaterThan(LocalDate.of(1990, 1, 1))).hasSize(1);
    check(criteria().dateOfBirth.greaterThan(LocalDate.of(2000, 1, 1))).empty();
    check(criteria().dateOfBirth.atMost(LocalDate.of(1990, 2, 2))).hasSize(1);
    check(criteria().dateOfBirth.atMost(LocalDate.of(1990, 2, 1))).empty();
    check(criteria().dateOfBirth.is(LocalDate.of(1990, 2, 2))).hasSize(1);
  }

  @Test
  public void intComparison() throws Exception {
    Person john = new PersonGenerator().next().withId("john").withFullName("John").withAge(30);
    insert(john);

    check(criteria().age.is(30)).hasSize(1);
    check(criteria().age.is(31)).empty();
    check(criteria().age.isNot(31)).hasSize(1);

    // at least
    check(criteria().age.atLeast(29)).hasSize(1);
    check(criteria().age.atLeast(30)).hasSize(1);
    check(criteria().age.atLeast(31)).empty();

    // at most
    check(criteria().age.atMost(31)).hasSize(1);
    check(criteria().age.atMost(30)).hasSize(1);
    check(criteria().age.atMost(29)).empty();

    check(criteria().age.greaterThan(29)).hasSize(1);
    check(criteria().age.greaterThan(30)).empty();
    check(criteria().age.greaterThan(31)).empty();

    check(criteria().age.in(Arrays.asList(1, 2, 3))).empty();
    check(criteria().age.in(1, 2, 3)).empty();
    check(criteria().age.in(29, 30, 31)).hasSize(1);
    check(criteria().age.in(Arrays.asList(29, 30, 31))).hasSize(1);
    check(criteria().age.notIn(1, 2, 3)).hasSize(1);
    check(criteria().age.notIn(39, 30, 31)).empty();

    check(criteria().age.atLeast(30).age.atMost(31)).hasSize(1);
    check(criteria().age.lessThan(30).age.greaterThan(31)).empty();

    // multiple filters on the same field
    check(criteria().age.is(30).age.greaterThan(31)).empty();
    check(criteria().age.is(30).age.isNot(30).or().age.is(30)).hasSize(1);
    check(criteria().age.is(30).age.greaterThan(30).or().age.is(31)).empty();

    // add second person
    Person adam = new PersonGenerator().next().withId("adam").withFullName("Adam").withAge(40);
    insert(adam);

    check(criteria().age.is(30)).toList().hasContentInAnyOrder(john);
    check(criteria().age.is(40)).toList().hasContentInAnyOrder(adam);
    check(criteria().age.atLeast(29)).toList().hasContentInAnyOrder(john, adam);
    check(criteria().age.atLeast(30)).toList().hasContentInAnyOrder(john, adam);
    check(criteria().age.atLeast(31)).toList().hasContentInAnyOrder(adam);
    check(criteria().age.atMost(31)).toList().hasContentInAnyOrder(john);
    check(criteria().age.atMost(30)).toList().hasContentInAnyOrder(john);
    check(criteria().age.atMost(29)).empty();
    check(criteria().age.greaterThan(29)).toList().hasContentInAnyOrder(john, adam);
    check(criteria().age.greaterThan(30)).toList().hasContentInAnyOrder(adam);
    check(criteria().age.greaterThan(31)).toList().hasContentInAnyOrder(adam);
    check(criteria().age.in(Arrays.asList(1, 2, 3))).empty();
    check(criteria().age.in(Arrays.asList(29, 30, 40, 44))).toList().hasContentInAnyOrder(john, adam);
    check(criteria().age.notIn(30, 31)).toList().hasContentInAnyOrder(adam);
    check(criteria().age.notIn(1, 2)).toList().hasContentInAnyOrder(john, adam);
    check(criteria().age.lessThan(1)).empty();
    check(criteria().age.lessThan(30)).empty();
    check(criteria().age.lessThan(31)).hasSize(1);
  }

  @Test
  public void between() {
    Person john = new PersonGenerator().next().withId("john").withFullName("John").withAge(30);
    insert(john);

    check(criteria().age.between(0, 40)).hasSize(1);
    check(criteria().age.between(30, 30)).hasSize(1);
    check(criteria().age.between(30, 31)).hasSize(1);
    check(criteria().age.between(29, 30)).hasSize(1);
    check(criteria().age.between(29, 31)).hasSize(1);
    check(criteria().age.between(30, 35)).hasSize(1);

    check(criteria().age.between(21, 29)).empty();
    check(criteria().age.between(29, 29)).empty();
    check(criteria().age.between(31, 31)).empty();
    check(criteria().age.between(31, 32)).empty();

    // invalid
    check(criteria().age.between(31, 30)).empty();
    check(criteria().age.between(32, 29)).empty();
  }

  /**
   * some variations of boolean logic with {@code NOT} operator
   */
  @Test
  public void notLogic() {
    Person john = new PersonGenerator().next().withId("john").withFullName("John").withAge(30);
    insert(john);

    check(criteria().not(p -> p.id.is("john"))).empty();
    check(criteria().not(p -> p.id.is("john").age.is(30))).empty();
    check(criteria().not(p -> p.id.is("john").age.is(31))).hasSize(1);
    check(criteria().not(p -> p.id.in("john", "john").age.in(30, 30))).empty();
    check(criteria().not(p -> p.id.in("john", "john").or().age.in(30, 30))).empty();
    check(criteria().not(p -> p.id.in("d", "d").age.in(99, 99))).hasSize(1);
    check(criteria().not(p -> p.id.in("d", "d").or().age.in(99, 99))).hasSize(1);

    check(criteria().not(p -> p.id.is("john1").or().id.is("john"))).empty();
    check(criteria().not(p -> p.id.is("john1").or().id.is("john2"))).hasSize(1);
    check(criteria().not(p -> p.id.isNot("john"))).hasSize(1);
    check(criteria().not(p -> p.age.atLeast(29).age.atMost(31))).empty();

    check(criteria().not(p -> p.age.is(30)).not(p2 -> p2.id.is("john"))).empty();
    check(criteria().not(p -> p.age.is(31)).not(p2 -> p2.id.is("DUMMY"))).hasSize(1);

    // double not
    check(criteria().not(p -> p.not(p2 -> p2.id.is("john")))).hasSize(1);

    // triple not
    check(criteria().not(p1 -> p1.not(p2 -> p2.not(p3 -> p3.id.is("john"))))).empty();
    check(criteria().not(p1 -> p1.not(p2 -> p2.not(p3 -> p3.id.isNot("john"))))).hasSize(1);

    check(criteria().not(p -> p.age.greaterThan(29))).empty();
    check(criteria().not(p -> p.age.greaterThan(31))).hasSize(1);

  }

  @Test
  public void basic() {
    assumeFeature(Feature.QUERY);

    final Person john = new PersonGenerator().next()
            .withFullName("John")
            .withIsActive(true)
            .withAge(22);

    insert(john);

    check(criteria().fullName.is("John")).hasSize(1);
    check(criteria().fullName.isNot("John")).empty();
    check(criteria().fullName.is("John")
            .age.isNot(1)).hasSize(1);
    check(criteria().fullName.is("John")
            .age.is(22)).hasSize(1);
    check(criteria().fullName.is("_MISSING_")).empty();
    check(criteria().fullName.in("John", "test2")).hasSize(1);
    check(criteria().fullName.notIn("John", "test2")).empty();

    // true / false
    check(criteria().isActive.isTrue()).hasSize(1);
    check(criteria().isActive.isFalse()).empty();

    // isPresent / isAbsent
    check(criteria().address.isAbsent()).empty();
    check(criteria().address.isPresent()).notEmpty();

    // simple OR
    check(criteria().address.isAbsent().or().address.isPresent()).notEmpty();
    check(criteria().isActive.isFalse().or().isActive.isTrue()).notEmpty();
  }

  @Test
  public void nested() {
    final ImmutablePerson john = new PersonGenerator().next().withBestFriend(ImmutableFriend.builder().hobby("ski").build());
    insert(john);
    final Address address = john.address().get();
    check(criteria().address.value().city.is(address.city())).notEmpty();
    check(criteria().address.value().zip.is(address.zip())).notEmpty();
    check(criteria().address.value().zip.is(address.zip())).toList().hasContentInAnyOrder(john);
    check(criteria().address.value().state.is(address.state())).toList().hasContentInAnyOrder(john);
    check(criteria().address.value().city.is("__MISSING__")).empty();
    check(criteria().bestFriend.value().hobby.is("ski")).notEmpty();
    check(criteria().bestFriend.value().hobby.is("__MISSING__")).empty();
  }

  @Test
  public void empty() {
    check(repository().findAll()).empty();
    check(repository().find(criteria())).empty();

    insert(new PersonGenerator().next());
    check(repository().findAll()).notEmpty();
    check(repository().find(criteria())).notEmpty();
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

    check(repository().findAll().orderBy(criteria().age.asc())).hasSize(count);
    check(repository().findAll().orderBy(criteria().age.asc()).limit(1)).toList(Person::fullName).isOf("name9");
    check(repository().findAll().orderBy(criteria().age.asc()).limit(2)).toList(Person::fullName).isOf("name9", "name8");
    check(repository().findAll().orderBy(criteria().age.asc()).limit(3)).toList(Person::fullName).isOf("name9", "name8", "name7");

    check(repository().findAll().orderBy(criteria().fullName.asc()).limit(1)).toList(Person::fullName).isOf("name0");
    check(repository().findAll().orderBy(criteria().fullName.asc()).limit(2)).toList(Person::fullName).isOf("name0", "name1");
    check(repository().findAll().orderBy(criteria().fullName.asc()).limit(3)).toList(Person::fullName).isOf("name0", "name1", "name2");

    check(repository().findAll().orderBy(criteria().age.desc())).hasSize(count);
    check(repository().findAll().orderBy(criteria().age.desc()).limit(1)).toList(Person::fullName).isOf("name0");
    check(repository().findAll().orderBy(criteria().age.desc()).limit(2)).toList(Person::fullName).isOf("name0", "name1");
    check(repository().findAll().orderBy(criteria().age.desc()).limit(3)).toList(Person::fullName).isOf("name0", "name1", "name2");

    check(repository().findAll().orderBy(criteria().fullName.desc())).hasSize(count);
    check(repository().findAll().orderBy(criteria().fullName.desc()).limit(1)).toList(Person::fullName).isOf("name9");
    check(repository().findAll().orderBy(criteria().fullName.desc()).limit(2)).toList(Person::fullName).isOf("name9", "name8");
    check(repository().findAll().orderBy(criteria().fullName.desc()).limit(3)).toList(Person::fullName).isOf("name9", "name8", "name7");


    check(repository().findAll().orderBy(criteria().fullName.asc(), criteria().age.asc()).limit(1)).toList(Person::fullName).isOf("name0");
    check(repository().findAll().orderBy(criteria().fullName.asc(), criteria().age.asc()).limit(2)).toList(Person::fullName).isOf("name0", "name1");
    check(repository().findAll().orderBy(criteria().fullName.desc(), criteria().age.asc()).limit(2)).toList(Person::fullName).isOf("name9", "name8");
    check(repository().findAll().orderBy(criteria().age.desc(), criteria().fullName.desc()).limit(2)).toList(Person::fullName).isOf("name0", "name1");


    assertOrdered(Person::age, repository().findAll().orderBy(criteria().age.asc()), Ordering.natural());
    assertOrdered(Person::age, repository().findAll().orderBy(criteria().age.asc()).limit(5), Ordering.natural());
    assertOrdered(Person::age, repository().findAll().orderBy(criteria().age.desc()), Ordering.natural().reverse());
    assertOrdered(Person::age, repository().findAll().orderBy(criteria().age.desc()).limit(5), Ordering.natural().reverse());
  }

  @Test
  public void regex() {
    assumeFeature(Feature.REGEX);
    final PersonGenerator generator = new PersonGenerator();
    insert(generator.next().withFullName("John"));

    check(repository().find(criteria().fullName.matches(Pattern.compile("John")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile("J.*n")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile("J..n")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile("J...")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile("...n")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile("^Jo")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile("hn$")))).hasSize(1);
    check(repository().find(criteria().fullName.not(s ->s.matches(Pattern.compile("J.*n"))))).empty();
    check(repository().find(criteria().fullName.matches(Pattern.compile("J\\w+n")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile(".*")))).hasSize(1);

    insert(generator.next().withFullName("Mary"));
    check(repository().find(criteria().fullName.matches(Pattern.compile("J.*n")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile("M.*ry")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile("^Ma")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile("ry$")))).hasSize(1);
    check(repository().find(criteria().fullName.matches(Pattern.compile(".*")))).hasSize(2);
  }

  @Test
  public void startsOrEndsWith() {
    assumeFeature(Feature.STRING_PREFIX_SUFFIX);
    final PersonGenerator generator = new PersonGenerator();
    insert(generator.next().withFullName("John"));

    // == starts with
    check(repository().find(criteria().fullName.startsWith(""))).hasSize(1);
    check(repository().find(criteria().fullName.startsWith("J"))).hasSize(1);
    check(repository().find(criteria().fullName.startsWith("Jo"))).hasSize(1);
    check(repository().find(criteria().fullName.startsWith("Jooo"))).empty();
    check(repository().find(criteria().fullName.startsWith("John"))).hasSize(1);
    check(repository().find(criteria().fullName.startsWith("j"))).empty();
    check(repository().find(criteria().fullName.startsWith("john"))).empty(); // not case sensitive

    // === ends with
    check(repository().find(criteria().fullName.endsWith(""))).hasSize(1);
    check(repository().find(criteria().fullName.endsWith("n"))).hasSize(1);
    check(repository().find(criteria().fullName.endsWith("hn"))).hasSize(1);
    check(repository().find(criteria().fullName.endsWith("ohn"))).hasSize(1);
    check(repository().find(criteria().fullName.endsWith("N"))).empty();

    insert(generator.next().withFullName("Mary"));
    check(repository().find(criteria().fullName.startsWith(""))).hasSize(2);
    check(repository().find(criteria().fullName.startsWith("Ma"))).hasSize(1);
    check(repository().find(criteria().fullName.startsWith("Mary"))).hasSize(1);
    check(repository().find(criteria().fullName.startsWith("Jo"))).hasSize(1);

    check(repository().find(criteria().fullName.endsWith(""))).hasSize(2);
    check(repository().find(criteria().fullName.endsWith("y"))).hasSize(1);
    check(repository().find(criteria().fullName.endsWith("Mary"))).hasSize(1);
  }

  @Test
  public void stringContains() {
    assumeFeature(Feature.STRING_PREFIX_SUFFIX);
    final PersonGenerator generator = new PersonGenerator();
    // on empty
    check(repository().find(criteria().fullName.contains(""))).empty();
    check(repository().find(criteria().fullName.contains("J"))).empty();
    check(repository().find(criteria().fullName.contains("John"))).empty();

    insert(generator.next().withFullName("John"));

//    check(repository().find(criteria().fullName.contains(""))).hasSize(1);
    check(repository().find(criteria().fullName.contains("J"))).hasSize(1);
    check(repository().find(criteria().fullName.contains("John"))).hasSize(1);
    check(repository().find(criteria().fullName.contains("John1"))).empty();
    check(repository().find(criteria().fullName.contains("Mary"))).empty();
    check(repository().find(criteria().fullName.contains("X"))).empty();
    check(repository().find(criteria().fullName.contains("oh"))).hasSize(1);
    check(repository().find(criteria().fullName.contains("ohn"))).hasSize(1);
    check(repository().find(criteria().fullName.contains("n"))).hasSize(1);
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

    // check(repository().find(criteria().pets.hasSize(0))).toList(Person::fullName).isOf("Mary", "John");
    check(repository().find(criteria().pets.hasSize(1))).toList(Person::fullName).hasContentInAnyOrder("Adam", "Paul");
    check(repository().find(criteria().pets.hasSize(2))).toList(Person::fullName).isOf("Emma");

    // negation
    check(repository().find(criteria().not(p -> p.pets.hasSize(1)))).toList(Person::fullName).not().isOf("Adam", "Paul");
    check(repository().find(criteria().not(p -> p.pets.hasSize(2)))).toList(Person::fullName).not().isOf("Emma");
  }

  @Test
  public void iterableContains() {
    assumeFeature(Feature.ITERABLE_CONTAINS);
    final PersonGenerator generator = new PersonGenerator();
    insert(generator.next().withFullName("John"));
    insert(generator.next().withFullName("Mary").withInterests("skiing"));
    insert(generator.next().withFullName("Adam").withInterests("hiking", "swimming"));
    insert(generator.next().withFullName("Emma").withInterests("cooking", "skiing"));

    check(repository().find(criteria().interests.contains("skiing"))).toList(Person::fullName).hasContentInAnyOrder("Mary", "Emma");
    check(repository().find(criteria().interests.contains("hiking"))).toList(Person::fullName).isOf("Adam");
    check(repository().find(criteria().interests.contains("cooking"))).toList(Person::fullName).isOf("Emma");
    check(repository().find(criteria().interests.contains("swimming"))).toList(Person::fullName).isOf("Adam");
    check(repository().find(criteria().interests.contains("dancing"))).empty();
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

    check(repository().find(criteria().nickName.hasLength(0))).toList(Person::fullName).hasContentInAnyOrder("Adam");
    check(repository().find(criteria().nickName.hasLength(1))).toList(Person::fullName).hasContentInAnyOrder("Mary");
    check(repository().find(criteria().nickName.hasLength(2))).toList(Person::fullName).hasContentInAnyOrder("Emma");
    check(repository().find(criteria().nickName.hasLength(3))).toList(Person::fullName).hasContentInAnyOrder("James");
    check(repository().find(criteria().nickName.hasLength(4))).toList(Person::fullName).isEmpty();
  }

  private void assumeFeature(Feature feature) {
    Assume.assumeTrue(features().contains(feature));
  }

  private <T extends Comparable<T>> void assertOrdered(Function<Person, T> extractor, Reader<Person, ?> reader, Ordering<T> ordering) {
    List<T> parts = fetch(reader).stream().map(extractor).collect(Collectors.toList());
    if (!ordering.isOrdered(parts)) {
      throw new AssertionError(String.format("%s is not ordered. Expected: %s", parts, ordering.sortedCopy(parts)));
    }
  }

  protected void insert(Person ... persons) {
    insert(Arrays.asList(persons));
  }

  protected void insert(Iterable<? extends Person> persons) {
    Flowable.fromPublisher(repository().insert(persons))
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertComplete();
  }

  protected CriteriaChecker<Person> check(Reader<Person, ?> reader) {
    return CriteriaChecker.of(reader);
  }

  private CriteriaChecker<Person> check(Criterion<Person> criterion) {
    return check(repository().find(criterion));
  }

  private List<Person> fetch(Reader<Person, ?> reader) {
    return Flowable.fromPublisher(((ReactiveReader<Person>) reader).fetch()).toList().blockingGet();
  }

}
