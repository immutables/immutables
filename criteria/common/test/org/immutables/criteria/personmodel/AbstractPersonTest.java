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
import org.junit.Assume;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.immutables.check.Checkers.check;

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
   * Called after setup
   */
  protected void populate() {

    final Person person = new PersonGenerator().next().withId("id123")
            .withFullName("test")
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

    Flowable.fromPublisher(repository().insert(new PersonGenerator().stream().limit(5).collect(Collectors.toList())))
            .singleOrError()
            .blockingGet();

    check(Flowable.fromPublisher(repository().findAll().limit(1).fetch()).toList().blockingGet()).hasSize(1);
    check(Flowable.fromPublisher(repository().findAll().limit(2).fetch()).toList().blockingGet()).hasSize(2);

    Assume.assumeTrue(features().contains(Feature.QUERY_WITH_OFFSET));
    check(Flowable.fromPublisher(repository().findAll().limit(1).offset(1).fetch()).toList().blockingGet()).hasSize(1);
    check(Flowable.fromPublisher(repository().findAll().limit(2).offset(2).fetch()).toList().blockingGet()).hasSize(2);
    check(Flowable.fromPublisher(repository().findAll().limit(1).offset(10).fetch()).toList().blockingGet()).isEmpty();
  }

  @Test
  public void comparison() {
    Assume.assumeTrue(features().contains(Feature.QUERY));

    execute(PersonCriteria.create().age.isAtLeast(22), 1);
    execute(PersonCriteria.create().age.isGreaterThan(22), 0);
    execute(PersonCriteria.create().age.isLessThan(22), 0);
    execute(PersonCriteria.create().age.isAtMost(22), 1);

    // look up using id
    execute(PersonCriteria.create().id.isEqualTo("id123"), 1);
    execute(PersonCriteria.create().id.isIn("foo", "bar", "id123"), 1);
    execute(PersonCriteria.create().id.isIn("foo", "bar", "qux"), 0);

    // jsr310. dates and time
    execute(PersonCriteria.create().dateOfBirth.isGreaterThan(LocalDate.of(1990, 1, 1)), 1);
    execute(PersonCriteria.create().dateOfBirth.isGreaterThan(LocalDate.of(2000, 1, 1)), 0);
    execute(PersonCriteria.create().dateOfBirth.isAtMost(LocalDate.of(1990, 2, 2)), 1);
    execute(PersonCriteria.create().dateOfBirth.isAtMost(LocalDate.of(1990, 2, 1)), 0);
    execute(PersonCriteria.create().dateOfBirth.isEqualTo(LocalDate.of(1990, 2, 2)), 1);

  }


  @Test
  public void basic() {
    execute(PersonCriteria.create().fullName.isEqualTo("test"), 1);
    execute(PersonCriteria.create().fullName.isNotEqualTo("test"), 0);
    execute(PersonCriteria.create().fullName.isEqualTo("test")
            .age.isNotEqualTo(1), 1);
    execute(PersonCriteria.create().fullName.isEqualTo("_MISSING_"), 0);
    execute(PersonCriteria.create().fullName.isIn("test", "test2"), 1);
    execute(PersonCriteria.create().fullName.isNotIn("test", "test2"), 0);
  }


  private void execute(Criterion<Person> expr, int count) {
    Flowable.fromPublisher(repository().find(expr).fetch())
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertValueCount(count);
  }

}
