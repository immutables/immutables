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
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

/**
 * Testing {@code COUNT(*)} functionality
 */
public abstract class CountTemplate {

  private final StringHolderRepository repository;
  private final Supplier<ImmutableStringHolder> generator;
  private final StringHolderCriteria string = StringHolderCriteria.stringHolder;

  protected CountTemplate(Backend backend) {
    this.repository = new StringHolderRepository(backend);
    this.generator = TypeHolder.StringHolder.generator();
  }

  @Test
  void empty() {
    check(repository.findAll().count()).is(0L);
    check(repository.find(string).count()).is(0L);
    check(repository.find(string.id.is("missing")).count()).is(0L);
  }

  @Test
  void basicCount() {
    repository.insert(generator.get().withValue("v1"));
    check(repository.findAll().count()).is(1L);

    repository.insert(generator.get().withValue("v2").withOptional("o2"));
    check(repository.findAll().count()).is(2L);

    repository.insert(generator.get().withValue("v3"));
    check(repository.findAll().count()).is(3L);

    check(repository.find(string.id.is("id1")).count()).is(1L);
    check(repository.find(string.id.is("BAD")).count()).is(0L);
    check(repository.find(string.value.is("BAD")).count()).is(0L);
    check(repository.find(string.value.is("v1")).count()).is(1L);
    check(repository.find(string.value.in("v1", "v2")).count()).is(2L);
    check(repository.find(string.value.in("v1", "v2", "v3")).count()).is(3L);

    // with projections. not very common but still possible
    check(repository.find(string.value.in("v1", "v2", "v3")).select(string.value).count()).is(3L);
    check(repository.find(string.value.in("v1", "v2", "v3"))
            .select(string.value, string.value).map((a, b) -> "a").count()).is(3L);
    check(repository.find(string.value.in("v1", "v2", "v3"))
            .select(string.value, string.value).map(tuple -> tuple.get(string.value)).count()).is(3L);
    check(repository.find(string.value.in("v1", "v2", "v3"))
            .select(string.value, string.value, string.value).map((a, b, c) -> "a").count()).is(3L);
  }

  @Test
  void countWithLimit() {
    check(repository.findAll().limit(1).count()).is(0L);
    check(repository.findAll().limit(2).count()).is(0L);

    repository.insert(generator.get().withValue("v1"));
    check(repository.findAll().limit(1).count()).is(1L);
    check(repository.findAll().limit(2).count()).is(1L);

    repository.insert(generator.get().withValue("v2"));
    check(repository.findAll().limit(1).count()).is(1L);
    check(repository.findAll().limit(2).count()).is(2L);
    check(repository.findAll().limit(3).count()).is(2L);

    repository.insert(generator.get().withValue("v3"));
    check(repository.findAll().limit(1).count()).is(1L);
    check(repository.findAll().limit(2).count()).is(2L);
    check(repository.findAll().limit(3).count()).is(3L);
    check(repository.findAll().limit(4).count()).is(3L);
  }
}
