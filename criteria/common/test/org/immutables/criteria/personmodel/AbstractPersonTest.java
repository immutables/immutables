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

import io.reactivex.Flowable;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.Repository;
import org.junit.Assume;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    WATCH
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
   * To be called after backend setup
   */
  protected void populate() {

    final Person person = new PersonGenerator().next()
            .withId("id123")
            .withFullName("test")
            .withIsActive(true)
            .withDateOfBirth(LocalDate.of(1990, 2, 2))
            .withAge(22);

    Flowable.fromPublisher(repository().insert(person))
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertComplete();
  }

  /**
   * limit and offset
   */
  @Test
  public void limit() {
    Assume.assumeTrue(features().contains(Feature.QUERY_WITH_LIMIT));

    final int size = 5;
    Flowable.fromPublisher(repository().insert(new PersonGenerator().stream().limit(size - 1).collect(Collectors.toList())))
            .singleOrError()
            .blockingGet();

    for (int i = 1; i < size * size; i++) {
      check(repository().findAll().limit(i)).hasSize(Math.min(i, size));
    }

    for (int i = 1; i < 3; i++) {
      check(repository().find(PersonCriteria.create().id.isEqualTo("id123")).limit(i)).hasSize(1);
    }

    Assume.assumeTrue(features().contains(Feature.QUERY_WITH_OFFSET));
    check(repository().findAll().limit(1).offset(1)).hasSize(1);
    check(repository().findAll().limit(2).offset(2)).hasSize(2);
    check(repository().findAll().limit(1).offset(size + 1)).empty();
  }

  @Test
  public void comparison() {
    Assume.assumeTrue(features().contains(Feature.QUERY));

    check(PersonCriteria.create().age.isAtLeast(22)).hasSize(1);
    check(PersonCriteria.create().age.isGreaterThan(22)).empty();
    check(PersonCriteria.create().age.isLessThan(22)).empty();
    check(PersonCriteria.create().age.isAtMost(22)).hasSize(1);

    // look up using id
    check(PersonCriteria.create().id.isEqualTo("id123")).hasSize(1);
    check(PersonCriteria.create().id.isIn("foo", "bar", "id123")).hasSize(1);
    check(PersonCriteria.create().id.isIn("foo", "bar", "qux")).empty();

    // jsr310. dates and time
    check(PersonCriteria.create().dateOfBirth.isGreaterThan(LocalDate.of(1990, 1, 1))).hasSize(1);
    check(PersonCriteria.create().dateOfBirth.isGreaterThan(LocalDate.of(2000, 1, 1))).empty();
    check(PersonCriteria.create().dateOfBirth.isAtMost(LocalDate.of(1990, 2, 2))).hasSize(1);
    check(PersonCriteria.create().dateOfBirth.isAtMost(LocalDate.of(1990, 2, 1))).empty();
    check(PersonCriteria.create().dateOfBirth.isEqualTo(LocalDate.of(1990, 2, 2))).hasSize(1);
  }


  @Test
  public void basic() {
    check(PersonCriteria.create().fullName.isEqualTo("test")).hasSize(1);
    check(PersonCriteria.create().fullName.isNotEqualTo("test")).empty();
    check(PersonCriteria.create().fullName.isEqualTo("test")
            .age.isNotEqualTo(1)).hasSize(1);
    check(PersonCriteria.create().fullName.isEqualTo("_MISSING_")).empty();
    check(PersonCriteria.create().fullName.isIn("test", "test2")).hasSize(1);
    check(PersonCriteria.create().fullName.isNotIn("test", "test2")).empty();

    // true / false
    check(PersonCriteria.create().isActive.isTrue()).hasSize(1);
    check(PersonCriteria.create().isActive.isFalse()).empty();

    // isPresent / isAbsent
    check(PersonCriteria.create().address.isAbsent()).empty();
    check(PersonCriteria.create().address.isPresent()).hasSize(1);
  }

  private CriteriaChecker<Person> check(Repository.Reader<Person, ?> reader) {
    return CriteriaChecker.of(reader);
  }

  private CriteriaChecker<Person> check(Criterion<Person> criterion) {
    return check(repository().find(criterion));
  }


}
