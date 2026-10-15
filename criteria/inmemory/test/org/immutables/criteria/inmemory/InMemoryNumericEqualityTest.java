/*
 * Copyright 2026 Immutables Authors and Contributors
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

package org.immutables.criteria.inmemory;

import org.immutables.check.IterableChecker;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.personmodel.CriteriaChecker;
import org.immutables.criteria.typemodel.BigDecimalHolderCriteria;
import org.immutables.criteria.typemodel.BigDecimalHolderRepository;
import org.immutables.criteria.typemodel.ImmutableBigDecimalHolder;
import org.immutables.criteria.typemodel.TypeHolder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

/**
 * Equality of decimals which differ in scale but not in value.
 *
 * <p>Scale is a property of the representation, not of the number: mongo selects a stored
 * {@code 2.00} for a queried {@code 2}, while {@link java.util.Objects#equals} does not. A backend
 * that answers this differently from the database it mirrors returns a different result set for
 * the same criterion.
 *
 * <p>Worth reading against ordering, which goes through {@link Comparable} and therefore already
 * treats the two as tied -- so today the same two values are unequal when filtered and equal when
 * sorted, inside one backend.
 *
 * <p>The same question is asked by {@code contains} over an iterable attribute, which is why it is
 * pinned here too: over a list of strings the two equalities agree and the defect stays invisible.
 */
class InMemoryNumericEqualityTest {

  private final Backend backend = new InMemoryBackend();
  private final BigDecimalHolderRepository repository = new BigDecimalHolderRepository(backend);
  private final BigDecimalHolderCriteria criteria = BigDecimalHolderCriteria.bigDecimalHolder;
  private final Supplier<ImmutableBigDecimalHolder> generator = TypeHolder.BigDecimalHolder.generator();

  @Test
  void equalityIgnoresScale() {
    insertFixture();

    ids(criteria.value.is(new BigDecimal("2"))).hasContentInAnyOrder("id1");
    ids(criteria.value.is(new BigDecimal("2.000"))).hasContentInAnyOrder("id1");
    ids(criteria.value.isNot(new BigDecimal("2"))).hasContentInAnyOrder("id2");
  }

  @Test
  void inIgnoresScale() {
    insertFixture();

    ids(criteria.value.in(new BigDecimal("2"), new BigDecimal("3.0"))).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.notIn(new BigDecimal("2"), new BigDecimal("42"))).hasContentInAnyOrder("id2");
    ids(criteria.value.notIn(new BigDecimal("2"), new BigDecimal("3.0"))).isEmpty();
  }

  @Test
  void containsIgnoresScale() {
    repository.insert(generator.get().withId("id1").withValue(BigDecimal.ONE).withList(new BigDecimal("2.00")));
    repository.insert(generator.get().withId("id2").withValue(BigDecimal.ONE).withList(new BigDecimal("5")));

    ids(criteria.list.contains(new BigDecimal("2"))).hasContentInAnyOrder("id1");
    ids(criteria.list.contains(new BigDecimal("5.0"))).hasContentInAnyOrder("id2");
    ids(criteria.list.contains(new BigDecimal("42"))).isEmpty();
  }

  private void insertFixture() {
    repository.insert(generator.get().withId("id1").withValue(new BigDecimal("2.00")));
    repository.insert(generator.get().withId("id2").withValue(new BigDecimal("3")));
  }

  private IterableChecker<List<String>, String> ids(BigDecimalHolderCriteria criteria) {
    return CriteriaChecker.<TypeHolder.BigDecimalHolder>ofReader(repository.find(criteria))
            .toList(TypeHolder.BigDecimalHolder::id);
  }
}
