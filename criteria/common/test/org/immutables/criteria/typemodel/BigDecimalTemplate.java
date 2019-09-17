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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

/**
 * Testing various string operations prefix/suffix/length etc.
 */
public abstract class BigDecimalTemplate {

  private final BigDecimalHolderRepository repository;
  private final BigDecimalHolderCriteria criteria = BigDecimalHolderCriteria.bigDecimalHolder;
  private final Supplier<ImmutableBigDecimalHolder> generator;

  protected BigDecimalTemplate(Backend backend) {
    this.repository = new BigDecimalHolderRepository(backend);
    this.generator = TypeHolder.BigDecimalHolder.generator();
  }

  @Test
  void equality() {
    BigDecimal zero = BigDecimal.ZERO;
    BigDecimal one = BigDecimal.ONE;
    BigDecimal two = BigDecimal.valueOf(2);
    repository.insert(generator.get().withId("id0").withValue(zero));
    repository.insert(generator.get().withId("id1").withValue(one));
    repository.insert(generator.get().withId("id2").withValue(two));

    ids(criteria.value.is(zero)).hasContentInAnyOrder("id0");
    ids(criteria.value.isNot(zero)).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.isNot(one)).hasContentInAnyOrder("id0", "id2");
    ids(criteria.value.is(one)).hasContentInAnyOrder("id1");
    ids(criteria.value.is(two)).hasContentInAnyOrder("id2");
    ids(criteria.value.is(BigDecimal.valueOf(42))).isEmpty();
    ids(criteria.value.in(zero, one)).hasContentInAnyOrder("id0", "id1");
    ids(criteria.value.in(zero, two)).hasContentInAnyOrder("id0", "id2");
    ids(criteria.value.notIn(zero, one, two)).isEmpty();
  }


  @Test
  void comparison() {
    BigDecimal zero = BigDecimal.ZERO;
    BigDecimal one = BigDecimal.ONE;
    BigDecimal two = BigDecimal.valueOf(2);

    repository.insert(generator.get().withId("id0").withValue(zero));
    repository.insert(generator.get().withId("id1").withValue(one));
    repository.insert(generator.get().withId("id2").withValue(two));

    ids(criteria.value.atMost(zero)).hasContentInAnyOrder("id0");
    ids(criteria.value.atLeast(zero)).hasContentInAnyOrder("id0", "id1", "id2");
    ids(criteria.value.atMost(two)).hasContentInAnyOrder("id0", "id1", "id2");
    ids(criteria.value.between(zero, two)).hasContentInAnyOrder("id0", "id1", "id2");
    ids(criteria.value.between(BigDecimal.valueOf(10), BigDecimal.valueOf(20))).isEmpty();
    ids(criteria.value.atMost(one)).hasContentInAnyOrder("id0", "id1");
    ids(criteria.value.atLeast(one)).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.atLeast(two)).hasContentInAnyOrder("id2");
    ids(criteria.value.atLeast(BigDecimal.valueOf(11))).isEmpty();
    ids(criteria.value.greaterThan(two)).isEmpty();
    ids(criteria.value.lessThan(zero)).isEmpty();
  }

  @Test
  protected void projection() {
    BigDecimal zero = BigDecimal.ZERO;
    BigDecimal one = BigDecimal.ONE;

    repository.insert(generator.get().withId("id1").withValue(zero).withNullable(null)
            .withOptional(Optional.empty()));

    repository.insert(generator.get().withId("id2").withValue(one).withNullable(one).withOptional(one));

    // projection of one attribute
    check(repository.findAll().select(criteria.value).fetch()).hasContentInAnyOrder(zero, one);
    check(repository.findAll().select(criteria.nullable).asOptional().fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of(one));
    check(repository.findAll().select(criteria.optional).fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of(one));

    // 4 attributes using mapper
    check(repository.findAll().select(criteria.id, criteria.value, criteria.nullable, criteria.optional)
            .map((id, value, nullable, optional) -> String.format("id=%s value=%s nullable=%s optional=%s", id, value, nullable, optional.orElse(null))).fetch())
            .hasContentInAnyOrder("id=id1 value=0 nullable=null optional=null", "id=id2 value=1 nullable=1 optional=1");

  }



  private IterableChecker<List<String>, String> ids(BigDecimalHolderCriteria criteria) {
    return CriteriaChecker.<TypeHolder.BigDecimalHolder>of(repository.find(criteria)).toList(TypeHolder.BigDecimalHolder::id);
  }
}
