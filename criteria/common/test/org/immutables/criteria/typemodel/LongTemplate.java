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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

/**
 * Testing various string operations prefix/suffix/length etc.
 */
public abstract class LongTemplate {

  private final LongHolderRepository repository;
  private final LongHolderCriteria criteria = LongHolderCriteria.longHolder;
  private final Supplier<ImmutableLongHolder> generator;

  protected LongTemplate(Backend backend) {
    this.repository = new LongHolderRepository(backend);
    this.generator = TypeHolder.LongHolder.generator();
  }

  @Test
  void empty() {
    final long zero = 0L;
    ids(criteria.value.is(zero)).isEmpty();
    ids(criteria.value.isNot(zero)).isEmpty();
    ids(criteria.value.atLeast(zero)).isEmpty();
    ids(criteria.value.atMost(zero)).isEmpty();
    ids(criteria.value.greaterThan(zero)).isEmpty();
    ids(criteria.value.lessThan(zero)).isEmpty();
    ids(criteria.value.between(zero, zero)).isEmpty();
  }

  @Test
  void equality() {
    repository.insert(generator.get().withId("id0").withValue(0L));
    repository.insert(generator.get().withId("id1").withValue(1L));
    repository.insert(generator.get().withId("id2").withValue(2L));

    ids(criteria.value.is(0L)).hasContentInAnyOrder("id0");
    ids(criteria.value.is(1L)).hasContentInAnyOrder("id1");
    ids(criteria.value.is(-1L)).isEmpty();
    ids(criteria.value.isNot(0L)).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.isNot(1L)).hasContentInAnyOrder("id0", "id2");
    ids(criteria.value.isNot(3L)).hasContentInAnyOrder("id0", "id1", "id2");
    ids(criteria.value.in(0L, 1L)).hasContentInAnyOrder("id1", "id0");
    ids(criteria.value.notIn(0L, 1L)).hasContentInAnyOrder("id2");
    ids(criteria.value.notIn(0L, 1L, 2L)).isEmpty();
    ids(criteria.value.notIn(-1L, -2L)).hasContentInAnyOrder("id0", "id1", "id2");
  }

  @Test
  void comparison() {
    repository.insert(generator.get().withId("id0").withValue(0L));
    repository.insert(generator.get().withId("id1").withValue(1L));
    repository.insert(generator.get().withId("id2").withValue(2L));

    ids(criteria.value.atMost(0L)).hasContentInAnyOrder("id0");
    ids(criteria.value.atLeast(0L)).hasContentInAnyOrder("id0", "id1", "id2");
    ids(criteria.value.atMost(2L)).hasContentInAnyOrder("id0", "id1", "id2");
    ids(criteria.value.between(0L, 2L)).hasContentInAnyOrder("id0", "id1", "id2");
    ids(criteria.value.between(10L, 20L)).isEmpty();
    ids(criteria.value.atMost(1L)).hasContentInAnyOrder("id0", "id1");
    ids(criteria.value.atLeast(1L)).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.atLeast(2L)).hasContentInAnyOrder("id2");
    ids(criteria.value.atLeast(33L)).isEmpty();
    ids(criteria.value.greaterThan(2L)).isEmpty();
    ids(criteria.value.lessThan(0L)).isEmpty();
  }

  @Test
  void projection() {
    repository.insert(generator.get().withId("id1").withValue(1).withNullable(null).withBoxed(1L)
            .withOptional(OptionalLong.empty()).withOptional2(Optional.of(1L)));

    repository.insert(generator.get().withId("id2").withValue(2).withNullable(2L).withBoxed(2L).withOptional(OptionalLong.of(2))
            .withOptional2(Optional.empty()));

    // projection of one attribute
    check(repository.findAll().select(criteria.id).fetch()).hasContentInAnyOrder("id1", "id2");
    check(repository.findAll().select(criteria.value).fetch()).hasContentInAnyOrder(1L, 2L);
    check(repository.findAll().select(criteria.nullable).asOptional().fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of(2L));
    check(repository.findAll().select(criteria.boxed).fetch()).hasContentInAnyOrder(1L, 2L);
    check(repository.findAll().select(criteria.optional).fetch()).hasContentInAnyOrder(OptionalLong.empty(), OptionalLong.of(2));
    check(repository.findAll().select(criteria.optional2).fetch()).hasContentInAnyOrder(Optional.of(1L), Optional.empty());
  }

  /**
   * Projection on a list type (eg. {@code List<Long>})
   */
  @Test
  protected void projectionOnIterable() {
    repository.insert(generator.get().withId("id1").withList()); // empty
    repository.insert(generator.get().withId("id2").withList(2)); // one element
    repository.insert(generator.get().withId("id3").withList(3, 4));

    // select all
    check(repository.findAll().select(criteria.list).fetch())
            .hasContentInAnyOrder(Collections.emptyList(), Collections.singletonList(2L), Arrays.asList(3L, 4L));

    // select by ID
    check(repository.find(criteria.id.is("id1")).select(criteria.list).one())
            .isEmpty();

    check(repository.find(criteria.id.is("id2")).select(criteria.list).one())
            .hasAll(2L);

    check(repository.find(criteria.id.is("id3")).select(criteria.list).one())
            .hasAll(3L, 4L);
  }


  private IterableChecker<List<String>, String> ids(LongHolderCriteria criteria) {
    return CriteriaChecker.<TypeHolder.LongHolder>ofReader(repository.find(criteria)).toList(TypeHolder.LongHolder::id);
  }
}
