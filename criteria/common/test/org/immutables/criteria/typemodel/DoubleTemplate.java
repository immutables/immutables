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
import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

public abstract class DoubleTemplate {

  private final DoubleHolderRepository repository;
  private final DoubleHolderCriteria criteria;
  private final Supplier<ImmutableDoubleHolder> generator;

  protected DoubleTemplate(Backend backend) {
    this.repository = new DoubleHolderRepository(backend);
    this.criteria = DoubleHolderCriteria.doubleHolder;
    this.generator = TypeHolder.DoubleHolder.generator();
  }

  @Test
  void equality() {
    repository.insert(generator.get().withId("id0").withValue(0D));
    repository.insert(generator.get().withId("id1").withValue(1D));
    repository.insert(generator.get().withId("id2").withValue(-1D));

    ids(criteria.value.is(0D)).hasContentInAnyOrder("id0");
    ids(criteria.value.isNot(0D)).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.isNot(1D)).hasContentInAnyOrder("id0", "id2");
    ids(criteria.value.is(1D)).hasContentInAnyOrder("id1");
    ids(criteria.value.is(-1D)).hasContentInAnyOrder("id2");
    ids(criteria.value.is(42D)).isEmpty();
    ids(criteria.value.in(0D, 1D)).hasContentInAnyOrder("id0", "id1");
    ids(criteria.value.in(0D, -1D)).hasContentInAnyOrder("id0", "id2");
    ids(criteria.value.notIn(0D, -1D, 1D)).isEmpty();
  }

  @Test
  void comparison() {
    repository.insert(generator.get().withId("id0").withValue(0D));
    repository.insert(generator.get().withId("id1").withValue(1D));
    repository.insert(generator.get().withId("id2").withValue(2D));

    ids(criteria.value.atMost(0D)).hasContentInAnyOrder("id0");
    ids(criteria.value.atLeast(0D)).hasContentInAnyOrder("id0", "id1", "id2");
    ids(criteria.value.atMost(2D)).hasContentInAnyOrder("id0", "id1", "id2");
    ids(criteria.value.between(0D, 2D)).hasContentInAnyOrder("id0", "id1", "id2");
    ids(criteria.value.between(10D, 20D)).isEmpty();
    ids(criteria.value.atMost(1D)).hasContentInAnyOrder("id0", "id1");
    ids(criteria.value.atLeast(1D)).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.atLeast(2D)).hasContentInAnyOrder("id2");
    ids(criteria.value.atLeast(33D)).isEmpty();
    ids(criteria.value.greaterThan(2D)).isEmpty();
    ids(criteria.value.lessThan(0D)).isEmpty();
  }

  @Test
  protected void projection() {
    repository.insert(generator.get().withId("id1").withValue(0D).withNullable(null).withBoxed(0D)
            .withOptional(OptionalDouble.empty()).withOptional2(Optional.of(0D)));

    repository.insert(generator.get().withId("id2").withValue(1D).withNullable(1D).withBoxed(1D).withOptional(OptionalDouble.of(1))
            .withOptional2(Optional.of(1D)));

    // projection of one attribute
    check(repository.findAll().select(criteria.id).fetch()).hasContentInAnyOrder("id1", "id2");
    check(repository.findAll().select(criteria.value).fetch()).hasContentInAnyOrder(0D, 1D);
    check(repository.findAll().select(criteria.nullable).asOptional().fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of(1D));
    check(repository.findAll().select(criteria.boxed).asOptional().fetch()).hasContentInAnyOrder(Optional.of(0D), Optional.of(1D));
    check(repository.findAll().select(criteria.optional).fetch()).hasContentInAnyOrder(OptionalDouble.empty(), OptionalDouble.of(1));
    check(repository.findAll().select(criteria.optional2).fetch()).hasContentInAnyOrder(Optional.of(0D), Optional.of(1D));

    // 5 attributes using mapper
    check(repository.findAll().select(criteria.id, criteria.nullable, criteria.boxed, criteria.optional, criteria.optional2)
            .map((id, nullable, boxed, optional, optional2) -> String.format("id=%s nullable=%s boxed=%s optional=%s optional2=%s", id, nullable, boxed, optional.orElse(-42), optional2.orElse(-42D))).fetch())
            .hasContentInAnyOrder("id=id1 nullable=null boxed=0.0 optional=-42.0 optional2=0.0", "id=id2 nullable=1.0 boxed=1.0 optional=1.0 optional2=1.0");

  }


  private IterableChecker<List<String>, String> ids(DoubleHolderCriteria criteria) {
    return CriteriaChecker.<TypeHolder.DoubleHolder>of(repository.find(criteria)).toList(TypeHolder.DoubleHolder::id);
  }

}
