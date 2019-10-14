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

import java.util.Collections;
import java.util.Objects;
import java.util.function.Supplier;

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
  void insertDuplicates() {
    ImmutableStringHolder holder1 = generator.get();
    repository.insert(holder1);

    ImmutableStringHolder holder2 = generator.get().withId(holder1.id());
    ImmutableStringHolder holder3 = generator.get().withId(holder1.id());
    // should throw exception for documents with same ID
    Assertions.assertThrows(BackendException.class, () -> repository.insert(holder2));
    Assertions.assertThrows(BackendException.class, () -> repository.insertAll(Collections.singleton(holder3)));

    // different id should be successful
    repository.insert(generator.get());
  }

  @Test
  void updateExisting() {
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
      repository.update(generator.get());
    }

    // same number of elements
    check(repository.findAll().fetch()).hasSize(1);
  }
}
