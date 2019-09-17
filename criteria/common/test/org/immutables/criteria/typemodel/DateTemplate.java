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

package org.immutables.criteria.typemodel;

import org.immutables.check.IterableChecker;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.personmodel.CriteriaChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

/**
 * Testing predicates, projections, sorting etc. on booleans
 */
public abstract class DateTemplate {

  private final DateHolderRepository repository;
  private final DateHolderCriteria criteria;
  private final Supplier<ImmutableDateHolder> generator;

  protected DateTemplate(Backend backend) {
    this.repository = new DateHolderRepository(backend);
    this.criteria = DateHolderCriteria.dateHolder;
    this.generator = TypeHolder.DateHolder.generator();
  }

  @Test
  void empty() {
    Date now = new Date();
    ids(criteria.value.is(now)).isEmpty();
    ids(criteria.value.isNot(now)).isEmpty();
    ids(criteria.value.in(now, now)).isEmpty();
    ids(criteria.value.notIn(now, now)).isEmpty();
    ids(criteria.value.atLeast(now)).isEmpty();
    ids(criteria.value.atMost(now)).isEmpty();
    ids(criteria.value.between(now, now)).isEmpty();
    ids(criteria.value.greaterThan(now)).isEmpty();
    ids(criteria.value.lessThan(now)).isEmpty();
  }

  @Test
  void equality() {
    final Date date1 = new Date();
    final Date other = new Date(date1.getTime() + TimeUnit.DAYS.toMillis(1));
    repository.insert(generator.get().withId("id1").withValue(date1));

    ids(criteria.value.is(date1)).hasContentInAnyOrder("id1");
    ids(criteria.value.is(other)).isEmpty();
    ids(criteria.value.isNot(date1)).isEmpty();
    ids(criteria.value.isNot(other)).hasContentInAnyOrder("id1");
    ids(criteria.value.in(date1, date1)).hasContentInAnyOrder("id1");
    ids(criteria.value.notIn(date1, date1)).isEmpty();
    ids(criteria.value.notIn(new Date(other.getTime() + 20_000), new Date(other.getTime() + 20_000))).hasContentInAnyOrder("id1");

    final Date date2 = new Date(other.getTime() + 30_000);
    repository.insert(generator.get().withId("id2").withValue(date2));
    ids(criteria.value.is(date2)).hasContentInAnyOrder("id2");
    ids(criteria.value.isNot(date2)).hasContentInAnyOrder("id1");
    ids(criteria.value.isNot(date1)).hasContentInAnyOrder("id2");
    ids(criteria.value.in(date1, date2)).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.in(new Date(date2.getTime() + 10_000), new Date(date2.getTime() + 20_000))).isEmpty();
    ids(criteria.value.notIn(date1, date2)).isEmpty();
    ids(criteria.value.notIn(new Date(date2.getTime() + 10_000), date2)).hasContentInAnyOrder("id1");
    ids(criteria.value.notIn(date1, new Date(date2.getTime() + 10_000))).hasContentInAnyOrder("id2");
  }

  @Test
  protected void comparison() {
    final Date date1 = new Date();
    final Date date2 = new Date(date1.getTime() + TimeUnit.DAYS.toMillis(7));
    // invariant date1 < date2
    Assertions.assertTrue(date1.compareTo(date2) < 0, String.format("Invariant: %s < %s", date1, date2));

    repository.insert(generator.get().withId("id1").withValue(date1));
    repository.insert(generator.get().withId("id2").withValue(date2));

    ids(criteria.value.greaterThan(date2)).isEmpty();
    ids(criteria.value.greaterThan(date1)).hasContentInAnyOrder("id2");
    ids(criteria.value.lessThan(date1)).isEmpty();
    ids(criteria.value.lessThan(date2)).hasContentInAnyOrder("id1");
    ids(criteria.value.between(date1, date2)).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.between(new Date(date2.getTime() + 10_000), new Date(date2.getTime() + 20_000))).isEmpty();
    ids(criteria.value.between(date2, date1)).isEmpty();
    ids(criteria.value.atMost(date1)).hasContentInAnyOrder("id1");
    ids(criteria.value.atMost(new Date(date1.getTime() - 10_000))).isEmpty();
    ids(criteria.value.atMost(date2)).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.atLeast(date1)).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.atLeast(date2)).hasContentInAnyOrder("id2");
    ids(criteria.value.atLeast(new Date(date2.getTime() + 30_000))).isEmpty();
  }


  @Test
  void nullable() {
    Date date = new Date();
    repository.insert(generator.get().withId("id1").withNullable(null));
    repository.insert(generator.get().withId("id2").withNullable(date));

    ids(criteria.nullable.isPresent()).hasContentInAnyOrder("id2");
    ids(criteria.nullable.isAbsent()).hasContentInAnyOrder("id1");
    ids(criteria.nullable.is(date)).hasContentInAnyOrder("id2");
    ids(criteria.nullable.atLeast(date)).hasContentInAnyOrder("id2");
    ids(criteria.nullable.atMost(date)).hasContentInAnyOrder("id2");
    ids(criteria.nullable.greaterThan(date)).isEmpty();
    ids(criteria.nullable.lessThan(date)).isEmpty();
  }

  @Test
  protected void optional() {
    Date date = new Date();
    repository.insert(generator.get().withId("id1").withOptional(Optional.empty()));
    repository.insert(generator.get().withId("id2").withOptional(Optional.of(date)));
    ids(criteria.optional.isPresent()).hasContentInAnyOrder("id2");
    ids(criteria.optional.isAbsent()).hasContentInAnyOrder("id1");
    ids(criteria.optional.is(date)).hasContentInAnyOrder("id2");
    ids(criteria.optional.atLeast(date)).hasContentInAnyOrder("id2");
    ids(criteria.optional.atMost(date)).hasContentInAnyOrder("id2");
    ids(criteria.optional.greaterThan(date)).isEmpty();
    ids(criteria.optional.lessThan(date)).isEmpty();
  }

  @Test
  void projection() {
    LocalDate reference = LocalDate.of(2010, 1, 1);
    Date now1 = Date.from(reference.atStartOfDay(ZoneId.systemDefault()).toInstant());
    Date now2 = Date.from(reference.plusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant());

    repository.insert(generator.get().withId("id1").withValue(now1).withNullable(null)
            .withOptional(Optional.empty()));
    repository.insert(generator.get().withId("id2").withValue(now2).withNullable(now2).withOptional(now2));

    // projection of one attribute
    check(repository.findAll().select(criteria.value).fetch()).hasContentInAnyOrder(now1, now2);
    check(repository.findAll().select(criteria.nullable).asOptional().fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of(now2));
    check(repository.findAll().select(criteria.optional).fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of(now2));

    final Function<Date, String> toStringFn = date ->  date == null ? "null" : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
    // 4 attributes using mapper
    check(repository.findAll().select(criteria.id, criteria.value, criteria.nullable, criteria.optional)
            .map((id, value, nullable, optional) -> String.format("id=%s value=%s nullable=%s optional=%s", id, toStringFn.apply(value), toStringFn.apply(nullable), toStringFn.apply(optional.orElse(null)))).fetch())
            .hasContentInAnyOrder("id=id1 value=2010-01-01 nullable=null optional=null", "id=id2 value=2010-01-03 nullable=2010-01-03 optional=2010-01-03");

  }

  private IterableChecker<List<String>, String> ids(DateHolderCriteria criteria) {
    return CriteriaChecker.<TypeHolder.DateHolder>of(repository.find(criteria)).toList(TypeHolder.DateHolder::id);
  }

}
