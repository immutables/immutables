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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

/**
 * Testing predicates, projections, sorting etc. on booleans
 */
public abstract class LocalDateTimeTemplate {

  private final LocalDateTimeHolderRepository repository;
  private final LocalDateTimeHolderCriteria holder;
  private final Supplier<ImmutableLocalDateTimeHolder> generator;

  protected LocalDateTimeTemplate(Backend backend) {
    this.repository = new LocalDateTimeHolderRepository(backend);
    this.holder = LocalDateTimeHolderCriteria.localDateTimeHolder;
    this.generator = TypeHolder.LocalDateTimeHolder.generator();
  }

  @Test
  void empty() {
    ids(holder.value.is(LocalDateTime.now())).isEmpty();
    ids(holder.value.isNot(LocalDateTime.now())).isEmpty();
    ids(holder.value.in(LocalDateTime.now(), LocalDateTime.now())).isEmpty();
    ids(holder.value.notIn(LocalDateTime.now(), LocalDateTime.now())).isEmpty();
    ids(holder.value.atLeast(LocalDateTime.now())).isEmpty();
    ids(holder.value.atMost(LocalDateTime.now())).isEmpty();
    ids(holder.value.between(LocalDateTime.now(), LocalDateTime.now())).isEmpty();
    ids(holder.value.greaterThan(LocalDateTime.now())).isEmpty();
    ids(holder.value.lessThan(LocalDateTime.now())).isEmpty();
  }

  @Test
  void equality() {
    final LocalDateTime date1 = LocalDateTime.now();
    repository.insert(generator.get().withId("id1").withValue(date1));

    ids(holder.value.is(date1)).hasContentInAnyOrder("id1");
    ids(holder.value.is(date1.plusDays(1))).isEmpty();
    ids(holder.value.isNot(date1)).isEmpty();
    ids(holder.value.isNot(date1.plusDays(1))).hasContentInAnyOrder("id1");
    ids(holder.value.in(date1, date1)).hasContentInAnyOrder("id1");
    ids(holder.value.notIn(date1, date1)).isEmpty();
    ids(holder.value.notIn(date1.plusMonths(1), date1.plusDays(1))).hasContentInAnyOrder("id1");

    final LocalDateTime date2 = LocalDateTime.now().plusWeeks(2);
    repository.insert(generator.get().withId("id2").withValue(date1).withValue(date2));
    ids(holder.value.is(date2)).hasContentInAnyOrder("id2");
    ids(holder.value.isNot(date2)).hasContentInAnyOrder("id1");
    ids(holder.value.isNot(date1)).hasContentInAnyOrder("id2");
    ids(holder.value.in(date1, date2)).hasContentInAnyOrder("id1", "id2");
    ids(holder.value.in(date1.plusDays(1), date2.plusDays(1))).isEmpty();
    ids(holder.value.notIn(date1, date2)).isEmpty();
    ids(holder.value.notIn(date1.plusDays(1), date2)).hasContentInAnyOrder("id1");
    ids(holder.value.notIn(date1, date2.plusDays(1))).hasContentInAnyOrder("id2");
  }

  @Test
  protected void comparison() {
    final LocalDateTime date1 = LocalDateTime.of(2010, 5, 1, 22, 0);
    final LocalDateTime date2 = LocalDateTime.of(2010, 10, 2, 22, 0);
    // invariant date1 < date2
    Assertions.assertTrue(date1.compareTo(date2) < 0, String.format("Invariant: %s < %s", date1, date2));

    repository.insert(generator.get().withId("id1").withValue(date1));
    repository.insert(generator.get().withId("id2").withValue(date2));

    ids(holder.value.greaterThan(date2)).isEmpty();
    ids(holder.value.greaterThan(date1)).hasContentInAnyOrder("id2");
    ids(holder.value.lessThan(date1)).isEmpty();
    ids(holder.value.lessThan(date2)).hasContentInAnyOrder("id1");
    ids(holder.value.between(date1, date2)).hasContentInAnyOrder("id1", "id2");
    ids(holder.value.between(date2.plusDays(1), date2.plusDays(2))).isEmpty();
    ids(holder.value.between(date2, date1)).isEmpty();
    ids(holder.value.atMost(date1)).hasContentInAnyOrder("id1");
    ids(holder.value.atMost(date1.minusDays(1))).isEmpty();
    ids(holder.value.atMost(date2)).hasContentInAnyOrder("id1", "id2");
    ids(holder.value.atLeast(date1)).hasContentInAnyOrder("id1", "id2");
    ids(holder.value.atLeast(date2)).hasContentInAnyOrder("id2");
    ids(holder.value.atLeast(date2.plusDays(1))).isEmpty();
  }


  @Test
  void nullable() {
    LocalDateTime date = LocalDateTime.now();
    repository.insert(generator.get().withId("id1").withNullable(null));
    repository.insert(generator.get().withId("id2").withNullable(date));

    ids(holder.nullable.isPresent()).hasContentInAnyOrder("id2");
    ids(holder.nullable.isAbsent()).hasContentInAnyOrder("id1");
    ids(holder.nullable.is(date)).hasContentInAnyOrder("id2");
    ids(holder.nullable.atLeast(date)).hasContentInAnyOrder("id2");
    ids(holder.nullable.atMost(date)).hasContentInAnyOrder("id2");
    ids(holder.nullable.greaterThan(date)).isEmpty();
    ids(holder.nullable.lessThan(date)).isEmpty();
  }

  @Test
  protected void optional() {
    LocalDateTime date = LocalDateTime.now();
    repository.insert(generator.get().withId("id1").withOptional(Optional.empty()));
    repository.insert(generator.get().withId("id2").withOptional(Optional.of(date)));
    ids(holder.optional.isPresent()).hasContentInAnyOrder("id2");
    ids(holder.optional.isAbsent()).hasContentInAnyOrder("id1");
    ids(holder.optional.is(date)).hasContentInAnyOrder("id2");
    ids(holder.optional.atLeast(date)).hasContentInAnyOrder("id2");
    ids(holder.optional.atMost(date)).hasContentInAnyOrder("id2");
    ids(holder.optional.greaterThan(date)).isEmpty();
    ids(holder.optional.lessThan(date)).isEmpty();
  }

  @Test
  void projection() {
    LocalDateTime now1 = LocalDateTime.of(2010, 1, 1, 18, 0);
    LocalDateTime now2 = now1.plusDays(2);
    repository.insert(generator.get().withId("id1").withValue(now1).withNullable(null)
            .withOptional(Optional.empty()));
    repository.insert(generator.get().withId("id2").withValue(now2).withNullable(now2).withOptional(now2));

    // projection of one attribute
    check(repository.findAll().select(holder.value).fetch()).hasContentInAnyOrder(now1, now2);
    check(repository.findAll().select(holder.nullable).asOptional().fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of(now2));
    check(repository.findAll().select(holder.optional).fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of(now2));

    // 4 attributes using mapper
    check(repository.findAll().select(holder.id, holder.value, holder.nullable, holder.optional)
            .map((id, value, nullable, optional) -> String.format("id=%s value=%s nullable=%s optional=%s", id, value, nullable, optional.orElse(null))).fetch())
            .hasContentInAnyOrder("id=id1 value=2010-01-01T18:00 nullable=null optional=null", "id=id2 value=2010-01-03T18:00 nullable=2010-01-03T18:00 optional=2010-01-03T18:00");

  }

  private IterableChecker<List<String>, String> ids(LocalDateTimeHolderCriteria criteria) {
    return CriteriaChecker.<TypeHolder.LocalDateTimeHolder>of(repository.find(criteria)).toList(TypeHolder.LocalDateTimeHolder::id);
  }

}
