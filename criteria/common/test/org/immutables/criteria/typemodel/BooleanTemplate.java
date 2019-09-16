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
import org.immutables.criteria.repository.sync.SyncReader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

/**
 * Testing predicates, projections, sorting etc. on booleans
 */
public class BooleanTemplate {

  private final BooleanHolderRepository repository;
  private final BooleanHolderCriteria holder;
  private final Supplier<ImmutableBooleanHolder> generator;

  protected BooleanTemplate(Backend backend) {
    this.repository = new BooleanHolderRepository(backend);
    this.holder = BooleanHolderCriteria.booleanHolder;
    this.generator = TypeHolder.BooleanHolder.generator();
  }

  /**
   * Check for empty results on empty backend
   */
  @Test
  void empty() {
    check(repository.find(holder.value.isFalse()).fetch()).isEmpty();
    check(repository.find(holder.value.isTrue()).fetch()).isEmpty();
    check(repository.find(holder.value.is(true)).fetch()).isEmpty();
    check(repository.find(holder.value.is(true)).fetch()).isEmpty();
    check(repository.find(holder.value.is(false)).fetch()).isEmpty();
    check(repository.find(holder.nullable.isTrue()).fetch()).isEmpty();
    check(repository.find(holder.nullable.isFalse()).fetch()).isEmpty();
    check(repository.find(holder.optional.isTrue()).fetch()).isEmpty();
    check(repository.find(holder.optional.isFalse()).fetch()).isEmpty();
  }

  @Test
  void basic() {
    repository.insert(generator.get().withId("id1").withValue(true));
    repository.insert(generator.get().withId("id2").withValue(false));

    values(repository.find(holder.value.is(true))).hasContentInAnyOrder("id1");
    values(repository.find(holder.value.isTrue())).hasContentInAnyOrder("id1");
    values(repository.find(holder.value.is(false))).hasContentInAnyOrder("id2");
    values(repository.find(holder.value.isFalse())).hasContentInAnyOrder("id2");
  }

  @Test
  protected void optional() {
    repository.insert(generator.get().withId("id1").withValue(true).withOptional(false));
    repository.insert(generator.get().withId("id2").withValue(false).withOptional(true));
    repository.insert(generator.get().withId("id3").withValue(false).withOptional(Optional.empty()));

    values(repository.find(holder.optional.isPresent())).hasContentInAnyOrder("id1", "id2");
    values(repository.find(holder.optional.isAbsent())).hasContentInAnyOrder("id3");
    values(repository.find(holder.optional.is(true))).hasContentInAnyOrder("id2");
    values(repository.find(holder.optional.isTrue())).hasContentInAnyOrder("id2");
    values(repository.find(holder.optional.is(false))).hasContentInAnyOrder("id1");
    values(repository.find(holder.optional.isFalse())).hasContentInAnyOrder("id1");
  }

  @Test
  void nullable() {
    repository.insert(generator.get().withId("id1").withValue(true).withNullable(false));
    repository.insert(generator.get().withId("id2").withValue(false).withNullable(true));
    repository.insert(generator.get().withId("id3").withValue(false).withNullable(null));

    values(repository.find(holder.nullable.isPresent())).hasContentInAnyOrder("id1", "id2");
    values(repository.find(holder.nullable.isAbsent())).hasContentInAnyOrder("id3");
    values(repository.find(holder.nullable.is(true))).hasContentInAnyOrder("id2");
    values(repository.find(holder.nullable.isTrue())).hasContentInAnyOrder("id2");
    values(repository.find(holder.nullable.is(false))).hasContentInAnyOrder("id1");
    values(repository.find(holder.nullable.isFalse())).hasContentInAnyOrder("id1");
  }

  private static IterableChecker<List<String>, String> values(SyncReader<TypeHolder.BooleanHolder> reader) {
    return CriteriaChecker.<TypeHolder.BooleanHolder>of(reader).toList(TypeHolder.BooleanHolder::id);
  }

}
