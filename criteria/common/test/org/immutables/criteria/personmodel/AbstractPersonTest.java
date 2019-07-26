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
import org.immutables.criteria.repository.ReactiveRepository;
import org.immutables.criteria.repository.Repository;
import org.junit.Assume;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
    ORDER_BY
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
  private static PersonCriteria<PersonCriteria.Self> criteria() {
    return PersonCriteria.person;
  }

  /**
   * limit and offset
   */
  @Test
  public void limit() {
    Assume.assumeTrue(features().contains(Feature.QUERY_WITH_LIMIT));
    final int size = 5;
    Flowable.fromPublisher(repository().insert(new PersonGenerator().stream()
            .limit(size).collect(Collectors.toList())))
            .singleOrError()
            .blockingGet();

    for (int i = 1; i < size * size; i++) {
      check(repository().findAll().limit(i)).hasSize(Math.min(i, size));
    }

    for (int i = 1; i < 3; i++) {
      check(repository().find(criteria().id.isEqualTo("id0")).limit(i)).hasSize(1);
    }

    Assume.assumeTrue(features().contains(Feature.QUERY_WITH_OFFSET));
    check(repository().findAll().limit(1).offset(1)).hasSize(1);
    check(repository().findAll().limit(2).offset(2)).hasSize(2);
    check(repository().findAll().limit(1).offset(size + 1)).empty();
  }

  @Test
  public void comparison() {
   Assume.assumeTrue(features().contains(Feature.QUERY));
   final Person john = new PersonGenerator().next()
            .withId("id123")
            .withDateOfBirth(LocalDate.of(1990, 2, 2))
            .withAge(22);

    insert(john);

    check(criteria().age.isAtLeast(22)).hasSize(1);
    check(criteria().age.isGreaterThan(22)).empty();
    check(criteria().age.isLessThan(22)).empty();
    check(criteria().age.isAtMost(22)).hasSize(1);

    // look up using id
    check(criteria().id.isEqualTo("id123")).hasSize(1);
    check(criteria().id.isIn("foo", "bar", "id123")).hasSize(1);
    check(criteria().id.isIn("foo", "bar", "qux")).empty();

    // jsr310. dates and time
    check(criteria().dateOfBirth.isGreaterThan(LocalDate.of(1990, 1, 1))).hasSize(1);
    check(criteria().dateOfBirth.isGreaterThan(LocalDate.of(2000, 1, 1))).empty();
    check(criteria().dateOfBirth.isAtMost(LocalDate.of(1990, 2, 2))).hasSize(1);
    check(criteria().dateOfBirth.isAtMost(LocalDate.of(1990, 2, 1))).empty();
    check(criteria().dateOfBirth.isEqualTo(LocalDate.of(1990, 2, 2))).hasSize(1);
  }

  @Test
  public void intComparison() throws Exception {
    Person john = new PersonGenerator().next().withId("john").withFullName("John").withAge(30);
    insert(john);

    check(criteria().age.isEqualTo(30)).hasSize(1);
    check(criteria().age.isEqualTo(31)).empty();
    check(criteria().age.isNotEqualTo(31)).hasSize(1);

    // at least
    check(criteria().age.isAtLeast(29)).hasSize(1);
    check(criteria().age.isAtLeast(30)).hasSize(1);
    check(criteria().age.isAtLeast(31)).empty();

    // at most
    check(criteria().age.isAtMost(31)).hasSize(1);
    check(criteria().age.isAtMost(30)).hasSize(1);
    check(criteria().age.isAtMost(29)).empty();

    check(criteria().age.isGreaterThan(29)).hasSize(1);
    check(criteria().age.isGreaterThan(30)).empty();
    check(criteria().age.isGreaterThan(31)).empty();

    check(criteria().age.isIn(Arrays.asList(1, 2, 3))).empty();
    check(criteria().age.isIn(1, 2, 3)).empty();
    check(criteria().age.isIn(29, 30, 31)).hasSize(1);
    check(criteria().age.isIn(Arrays.asList(29, 30, 31))).hasSize(1);
    check(criteria().age.isNotIn(1, 2, 3)).hasSize(1);
    check(criteria().age.isNotIn(39, 30, 31)).empty();

    check(criteria().age.isAtLeast(30).age.isAtMost(31)).hasSize(1);
    check(criteria().age.isLessThan(30).age.isGreaterThan(31)).empty();

    // multiple filters on the same field
    check(criteria().age.isEqualTo(30).age.isGreaterThan(31)).empty();
    check(criteria().age.isEqualTo(30).age.isNotEqualTo(30).or().age.isEqualTo(30)).hasSize(1);
    check(criteria().age.isEqualTo(30).age.isGreaterThan(30).or().age.isEqualTo(31)).empty();

    // add second person
    Person adam = new PersonGenerator().next().withId("adam").withFullName("Adam").withAge(40);
    insert(adam);

    check(criteria().age.isEqualTo(30)).toList().hasContentInAnyOrder(john);
    check(criteria().age.isEqualTo(40)).toList().hasContentInAnyOrder(adam);
    check(criteria().age.isAtLeast(29)).toList().hasContentInAnyOrder(john, adam);
    check(criteria().age.isAtLeast(30)).toList().hasContentInAnyOrder(john, adam);
    check(criteria().age.isAtLeast(31)).toList().hasContentInAnyOrder(adam);
    check(criteria().age.isAtMost(31)).toList().hasContentInAnyOrder(john);
    check(criteria().age.isAtMost(30)).toList().hasContentInAnyOrder(john);
    check(criteria().age.isAtMost(29)).empty();
    check(criteria().age.isGreaterThan(29)).toList().hasContentInAnyOrder(john, adam);
    check(criteria().age.isGreaterThan(30)).toList().hasContentInAnyOrder(adam);
    check(criteria().age.isGreaterThan(31)).toList().hasContentInAnyOrder(adam);
    check(criteria().age.isIn(Arrays.asList(1, 2, 3))).empty();
    check(criteria().age.isIn(Arrays.asList(29, 30, 40, 44))).toList().hasContentInAnyOrder(john, adam);
    check(criteria().age.isNotIn(30, 31)).toList().hasContentInAnyOrder(adam);
    check(criteria().age.isNotIn(1, 2)).toList().hasContentInAnyOrder(john, adam);
    check(criteria().age.isLessThan(1)).empty();
    check(criteria().age.isLessThan(30)).empty();
    check(criteria().age.isLessThan(31)).hasSize(1);

  }

  /**
   * some variations of boolean logic with {@code NOT} operator
   */
  @Test
  public void notLogic() {
    Person john = new PersonGenerator().next().withId("john").withFullName("John").withAge(30);
    insert(john);

    check(criteria().not(p -> p.id.isEqualTo("john"))).empty();
    check(criteria().not(p -> p.id.isEqualTo("john").age.isEqualTo(30))).empty();
    check(criteria().not(p -> p.id.isEqualTo("john").age.isEqualTo(31))).hasSize(1);
    check(criteria().not(p -> p.id.isIn("john", "john").age.isIn(30, 30))).empty();
    check(criteria().not(p -> p.id.isIn("john", "john").or().age.isIn(30, 30))).empty();
    check(criteria().not(p -> p.id.isIn("d", "d").age.isIn(99, 99))).hasSize(1);
    check(criteria().not(p -> p.id.isIn("d", "d").or().age.isIn(99, 99))).hasSize(1);

    check(criteria().not(p -> p.id.isEqualTo("john1").or().id.isEqualTo("john"))).empty();
    check(criteria().not(p -> p.id.isEqualTo("john1").or().id.isEqualTo("john2"))).hasSize(1);
    check(criteria().not(p -> p.id.isNotEqualTo("john"))).hasSize(1);
    check(criteria().not(p -> p.age.isAtLeast(29).age.isAtMost(31))).empty();

    check(criteria().not(p -> p.age.isEqualTo(30)).not(p2 -> p2.id.isEqualTo("john"))).empty();
    check(criteria().not(p -> p.age.isEqualTo(31)).not(p2 -> p2.id.isEqualTo("DUMMY"))).hasSize(1);

    // double not
    check(criteria().not(p -> p.not(p2 -> p2.id.isEqualTo("john")))).hasSize(1);

    // triple not
    check(criteria().not(p1 -> p1.not(p2 -> p2.not(p3 -> p3.id.isEqualTo("john"))))).empty();
    check(criteria().not(p1 -> p1.not(p2 -> p2.not(p3 -> p3.id.isNotEqualTo("john"))))).hasSize(1);

    check(criteria().not(p -> p.age.isGreaterThan(29))).empty();
    check(criteria().not(p -> p.age.isGreaterThan(31))).hasSize(1);

  }

  @Test
  public void basic() {
    Assume.assumeTrue(features().contains(Feature.QUERY));

    final Person john = new PersonGenerator().next()
            .withFullName("John")
            .withIsActive(true)
            .withAge(22);

    insert(john);

    check(criteria().fullName.isEqualTo("John")).hasSize(1);
    check(criteria().fullName.isNotEqualTo("John")).empty();
    check(criteria().fullName.isEqualTo("John")
            .age.isNotEqualTo(1)).hasSize(1);
    check(criteria().fullName.isEqualTo("John")
            .age.isEqualTo(22)).hasSize(1);
    check(criteria().fullName.isEqualTo("_MISSING_")).empty();
    check(criteria().fullName.isIn("John", "test2")).hasSize(1);
    check(criteria().fullName.isNotIn("John", "test2")).empty();

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
    check(criteria().address.value().city.isEqualTo(address.city())).notEmpty();
    check(criteria().address.value().zip.isEqualTo(address.zip())).notEmpty();
    check(criteria().address.value().zip.isEqualTo(address.zip())).toList().hasContentInAnyOrder(john);
    check(criteria().address.value().state.isEqualTo(address.state())).toList().hasContentInAnyOrder(john);
    check(criteria().address.value().city.isEqualTo("__MISSING__")).empty();
    check(criteria().bestFriend.value().hobby.isEqualTo("ski")).notEmpty();
    check(criteria().bestFriend.value().hobby.isEqualTo("__MISSING__")).empty();
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
    Assume.assumeTrue(features().contains(Feature.ORDER_BY));
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

  private <T extends Comparable<T>> void assertOrdered(Function<Person, T> extractor, Repository.Reader<Person, ?> reader, Ordering<T> ordering) {
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

  private CriteriaChecker<Person> check(Repository.Reader<Person, ?> reader) {
    return CriteriaChecker.of(reader);
  }

  private CriteriaChecker<Person> check(Criterion<Person> criterion) {
    return check(repository().find(criterion));
  }

  private List<Person> fetch(Repository.Reader<Person, ?> reader) {
    return Flowable.fromPublisher(((ReactiveRepository.Reader<Person>) reader).fetch()).toList().blockingGet();
  }

}
