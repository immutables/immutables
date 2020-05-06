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

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.BackendException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.immutables.check.Checkers.check;


public abstract class WriteTemplate {
  private final StringHolderRepository repository;
  private final Supplier<ImmutableStringHolder> generator;
  private final StringHolderCriteria stringHolder = StringHolderCriteria.stringHolder;

  protected WriteTemplate(Backend backend) {
    Objects.requireNonNull(backend, "backend");
    this.repository = new StringHolderRepository(backend);
    this.generator = TypeHolder.StringHolder.generator();;
  }

  @Test
  protected void insert() {
    ImmutableStringHolder holder1 = generator.get();
    repository.insert(holder1);

    ImmutableStringHolder holder2 = generator.get().withId(holder1.id());
    ImmutableStringHolder holder3 = generator.get().withId(holder1.id());
    // should throw exception for documents with same ID
    Assertions.assertThrows(BackendException.class, () -> repository.insert(holder2));
    Assertions.assertThrows(BackendException.class, () -> repository.insertAll(Collections.singleton(holder3)));

    // different id should be successful
    repository.insert(generator.get());
    repository.insertAll(Arrays.asList(generator.get(), generator.get()));
  }

  @Test
  protected void update() {
    ImmutableStringHolder holder1 = generator.get();
    repository.insert(holder1);

    repository.update(holder1.withValue("v1_changed").withOptional("v2_changed").withNullable("v3_changed"));

    for (int i = 0; i < 3; i++) {
      TypeHolder.StringHolder holder2 = repository.findAll().one();
      check(holder2.id()).is(holder1.id());
      check(holder2.value()).is("v1_changed");
      check(holder2.optional().get()).is("v2_changed");
      check(holder2.nullable()).is("v3_changed");
      // try to update something that doesn't exists
      // should not change existing record
      repository.updateAll(Collections.singleton(generator.get()));
    }

    // same number of elements
    check(repository.findAll().fetch()).hasSize(1);
  }

  @Test
  protected void upsert() {
    ImmutableStringHolder holder1 = generator.get().withId("id1");
    repository.upsert(holder1);

    check(repository.findAll().fetch()).hasSize(1);

    // do upsert
    repository.upsert(holder1.withValue("v1_changed").withOptional("v2_changed").withNullable("v3_changed"));

    // still size == 1
    check(repository.findAll().fetch()).hasSize(1);
    TypeHolder.StringHolder holder2 = repository.findAll().one();
    check(holder2.id()).is(holder1.id());
    check(holder2.value()).is("v1_changed");
    check(holder2.optional().get()).is("v2_changed");
    check(holder2.nullable()).is("v3_changed");

    repository.upsertAll(Collections.singleton(generator.get().withId("id2")));
    List<TypeHolder.StringHolder> all = repository.findAll().fetch();
    check(all).hasSize(2);
    check(all.stream().map(TypeHolder.StringHolder::id).collect(Collectors.toList())).hasContentInAnyOrder("id1", "id2");
  }

  @Test
  protected void delete() {
    repository.insert(generator.get().withId("id1").withValue("value1"));
    repository.insert(generator.get().withId("id2").withValue("value2"));

    repository.delete(stringHolder.value.is("__MISSING__"));
    check(repository.findAll().fetch()).hasSize(2);

    repository.delete(stringHolder.id.is("id1"));
    check(repository.findAll().fetch()).hasSize(1);

    repository.delete(stringHolder.value.in(Collections.singleton("value2")));
    check(repository.findAll().fetch()).isEmpty();
  }

  /**
   * Delete all records in bulk
   */
  @Test
  void deleteAll() {
    repository.insert(generator.get().withId("id1").withValue("value1"));
    repository.insert(generator.get().withId("id2").withValue("value2"));
    check(repository.findAll().fetch()).notEmpty();

    // TODO add deleteAll method to repository ?
    repository.delete(stringHolder);

    check(repository.findAll().fetch()).isEmpty();
  }
}
