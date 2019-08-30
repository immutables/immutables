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
import org.immutables.check.Checkers;
import org.immutables.check.IterableChecker;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.repository.reactive.ReactiveFetcher;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public abstract class PersonAggregationTest {

  private final PersonGenerator generator = new PersonGenerator();

  private final PersonCriteria person = PersonCriteria.person;

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

  @Test
  public void empty() {
    check(repository().findAll().groupBy(person.nickName)
            .select(person.nickName, person.age.sum())
            .map((nickName, age) -> true) // ignore
    ).isEmpty();

    check(repository().findAll()
            .orderBy(person.nickName.asc())
            .groupBy(person.nickName)
            .select(person.nickName, person.age.sum())
            .map((nickName, age) -> true) // ignore
    ).isEmpty();

    check(repository().findAll()
            .orderBy(person.nickName.desc())
            .groupBy(person.nickName)
            .select(person.nickName, person.age.max())
            .map((nickName, age) -> true) // ignore
    ).isEmpty();
  }

  @Test
  public void agg1() {
    insert(generator.next().withNickName("a").withAge(20));
    insert(generator.next().withNickName("a").withAge(30));
    insert(generator.next().withNickName("b").withAge(40));
    insert(generator.next().withNickName(Optional.empty()).withAge(10));

    check(repository().findAll()
            .groupBy(person.nickName)
            .select(person.nickName, person.age.sum())
            .map((nickName, age) -> (nickName.orElse(null) + "." + age.intValue())))
            .hasContentInAnyOrder("a.50", "b.40", "null.10");

    check(repository().findAll()
            .orderBy(person.nickName.asc())
            .groupBy(person.nickName)
            .select(person.nickName, person.age.sum())
            .map((nickName, age) -> (nickName.orElse(null) + "." + age.intValue())))
            .isOf("null.10", "a.50", "b.40");

    check(repository().findAll()
            .orderBy(person.nickName.desc())
            .groupBy(person.nickName)
            .select(person.nickName, person.age.count())
            .map((nickName, age) -> (nickName.orElse(null) + "." + age)))
            .isOf("b.1", "a.2", "null.1");

    check(repository().findAll()
            .orderBy(person.nickName.desc())
            .groupBy(person.nickName)
            .select(person.nickName, person.age.max(), person.age.min(), person.age.count(), person.age.sum())
            .map((nickName, max, min, count, sum) -> ("nick=" + nickName.orElse(null) + " max=" + max + " min=" + min + " count=" + count + " sum=" + sum.intValue())))
            .isOf("nick=b max=40 min=40 count=1 sum=40", "nick=a max=30 min=20 count=2 sum=50", "nick=null max=10 min=10 count=1 sum=10");

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

  <T> IterableChecker<List<T>, T> check(ReactiveFetcher<T> fetcher) {
    return Checkers.check(Flowable.fromPublisher(fetcher.fetch()).toList().blockingGet());
  }

}
