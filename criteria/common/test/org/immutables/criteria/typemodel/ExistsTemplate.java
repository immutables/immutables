/*
 * Copyright 2020 Immutables Authors and Contributors
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
 * Testing {@code exists} functionality, ie wherever at least one record could be found
 * for a given criteria.
 */
public abstract class ExistsTemplate {

  private final StringHolderRepository repository;
  private final Supplier<ImmutableStringHolder> generator;
  private final StringHolderCriteria string = StringHolderCriteria.stringHolder;

  protected ExistsTemplate(Backend backend) {
    this.repository = new StringHolderRepository(backend);
    this.generator = TypeHolder.StringHolder.generator();
  }

  @Test
  void empty() {
    check(!repository.findAll().exists());
    check(!repository.find(string).exists());
    check(!repository.find(string.id.is("missing")).exists());
  }

  @Test
  void basicExists() {
    check(!repository.findAll().exists());

    repository.insert(generator.get().withId("id1").withValue("v1"));
    check(repository.findAll().exists());

    check(repository.find(string.id.is("id1")).exists());
    check(!repository.find(string.id.is("idBAD")).exists());
    check(repository.find(string.id.isNot("idBAD")).exists());
    check(!repository.find(string.id.isNot("id1")).exists());

    check(repository.find(string.value.is("v1")).exists());
    check(!repository.find(string.value.is("v2")).exists());
    check(!repository.find(string.value.isNot("v1")).exists());
    check(repository.find(string.value.isNot("v2")).exists());

    repository.insert(generator.get().withId("id2").withValue("v2").withOptional("o2"));
    check(repository.findAll().exists());
    check(repository.find(string.id.is("id2")).exists());
    check(repository.find(string.id.in("id1", "id2")).exists());
    check(!repository.find(string.id.notIn("id1", "id2")).exists());

    repository.insert(generator.get().withValue("v3"));
    check(repository.findAll().exists());

    check(!repository.find(string.id.is("BAD")).exists());
    check(!repository.find(string.value.is("BAD")).exists());
    check(!repository.find(string.value.in("BAD1", "BAD2")).exists());
    check(repository.find(string.value.is("v1")).exists());
    check(repository.find(string.value.in("v1", "v2")).exists());
    check(repository.find(string.value.in("v1", "BAD")).exists());
    check(repository.find(string.value.in("BAD", "v1")).exists());
    check(repository.find(string.value.in("v1", "v2", "v3")).exists());

    // with projections. not very common but still possible
    check(repository.find(string.value.in("v1", "v2", "v3")).select(string.value).exists());
    check(repository.find(string.value.in("v1", "v2", "v3"))
            .select(string.value, string.value).map((a, b) -> "a").exists());
    check(repository.find(string.value.in("v1", "v2", "v3"))
            .select(string.value, string.value).map(tuple -> tuple.get(string.value)).exists());
    check(repository.find(string.value.in("v1", "v2", "v3"))
            .select(string.value, string.value, string.value).map((a, b, c) -> "a").exists());
  }

  @Test
  void existsWithLimit() {
    // empty
    check(!repository.findAll().limit(0).exists());
    check(!repository.findAll().limit(1).exists());
    check(!repository.findAll().limit(2).exists());

    repository.insert(generator.get().withId("id1").withValue("v1"));
    check(!repository.findAll().limit(0).exists());
    check(repository.findAll().limit(1).exists());
    check(repository.findAll().limit(2).exists());

    check(!repository.find(string.id.is("id1")).limit(0).exists());
    check(repository.find(string.id.is("id1")).limit(1).exists());
    check(repository.find(string.id.is("id1")).limit(2).exists());
  }
}