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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

public abstract class IntegerTemplate {

  private final IntegerHolderRepository repository;
  private final IntegerHolderCriteria criteria;
  private final Supplier<ImmutableIntegerHolder> generator;

  protected IntegerTemplate(Backend backend) {
    this.repository = new IntegerHolderRepository(backend);
    this.criteria = IntegerHolderCriteria.integerHolder;
    this.generator = TypeHolder.IntegerHolder.generator();
  }


  @Test
  void equality() {
    repository.insert(generator.get().withId("id0").withValue(0));
    repository.insert(generator.get().withId("id1").withValue(1));
    repository.insert(generator.get().withId("id2").withValue(-1));

    ids(criteria.value.is(0)).hasContentInAnyOrder("id0");
    ids(criteria.value.isNot(0)).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.isNot(1)).hasContentInAnyOrder("id0", "id2");
    ids(criteria.value.is(1)).hasContentInAnyOrder("id1");
    ids(criteria.value.is(-1)).hasContentInAnyOrder("id2");
    ids(criteria.value.is(42)).isEmpty();
    ids(criteria.value.in(0, 1)).hasContentInAnyOrder("id0", "id1");
    ids(criteria.value.in(0, -1)).hasContentInAnyOrder("id0", "id2");
    ids(criteria.value.notIn(0, -1, 1)).isEmpty();
  }

  @Test
  void comparison() {
    repository.insert(generator.get().withId("id0").withValue(0));
    repository.insert(generator.get().withId("id1").withValue(1));
    repository.insert(generator.get().withId("id2").withValue(2));

    ids(criteria.value.atMost(0)).hasContentInAnyOrder("id0");
    ids(criteria.value.atLeast(0)).hasContentInAnyOrder("id0", "id1", "id2");
    ids(criteria.value.atMost(2)).hasContentInAnyOrder("id0", "id1", "id2");
    ids(criteria.value.between(0, 2)).hasContentInAnyOrder("id0", "id1", "id2");
    ids(criteria.value.between(10, 20)).isEmpty();
    ids(criteria.value.atMost(1)).hasContentInAnyOrder("id0", "id1");
    ids(criteria.value.atLeast(1)).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.atLeast(2)).hasContentInAnyOrder("id2");
    ids(criteria.value.atLeast(33)).isEmpty();
    ids(criteria.value.greaterThan(2)).isEmpty();
    ids(criteria.value.lessThan(0)).isEmpty();
  }

  @Test
  protected void projection() {
    repository.insert(generator.get().withId("id1").withValue(0).withNullable(null).withBoxed(0)
            .withOptional(OptionalInt.empty()).withOptional2(Optional.of(0)));

    repository.insert(generator.get().withId("id2").withValue(1).withNullable(1).withBoxed(1).withOptional(OptionalInt.of(1))
            .withOptional2(Optional.of(1)));

    // projection of one attribute
    check(repository.findAll().select(criteria.value).fetch()).hasContentInAnyOrder(0, 1);
    check(repository.findAll().select(criteria.nullable).asOptional().fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of(1));
    check(repository.findAll().select(criteria.boxed).fetch()).hasContentInAnyOrder(0, 1);
    check(repository.findAll().select(criteria.optional).fetch()).hasContentInAnyOrder(OptionalInt.empty(), OptionalInt.of(1));
    check(repository.findAll().select(criteria.optional2).fetch()).hasContentInAnyOrder(Optional.of(0), Optional.of(1));

    // 5 attributes using mapper
    check(repository.findAll().select(criteria.id, criteria.nullable, criteria.boxed, criteria.optional, criteria.optional2)
            .map((id, nullable, boxed, optional, optional2) -> String.format("id=%s nullable=%s boxed=%s optional=%s optional2=%s", id, nullable, boxed, optional.orElse(-42), optional2.orElse(-42))).fetch())
            .hasContentInAnyOrder("id=id1 nullable=null boxed=0 optional=-42 optional2=0", "id=id2 nullable=1 boxed=1 optional=1 optional2=1");
  }


  private IterableChecker<List<String>, String> ids(IntegerHolderCriteria criteria) {
    return CriteriaChecker.<TypeHolder.IntegerHolder>of(repository.find(criteria)).toList(TypeHolder.IntegerHolder::id);
  }

}
