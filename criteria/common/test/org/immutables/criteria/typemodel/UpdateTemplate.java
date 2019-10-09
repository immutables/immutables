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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

/**
 * Tests for update operation
 */
public abstract class UpdateTemplate {


  private final StringHolderRepository repository;
  private final Supplier<ImmutableStringHolder> generator;
  private final StringHolderCriteria stringHolder = StringHolderCriteria.stringHolder;

  protected UpdateTemplate(Backend backend) {
    Objects.requireNonNull(backend, "backend");
    this.repository = new StringHolderRepository(backend);
    this.generator = TypeHolder.StringHolder.generator();;
  }

  @Test
  void simple() {
    repository.update(stringHolder).set(stringHolder.value, "aaa").execute();
    check(repository.findAll().fetch()).isEmpty();

    repository.insert(generator.get().withId("id1").withValue("TO_BE_CHANGED"));
    repository.update(stringHolder).set(stringHolder.value, "aaa").execute();
    check(repository.findAll().one().value()).is("aaa");

    repository.update(stringHolder).set(stringHolder.value, "bbb").execute();
    check(repository.findAll().one().value()).is("bbb");

    // different id which does not match any document
    repository.update(stringHolder.id.is("id2")).set(stringHolder.value, "SHOULD_NOT_BE_CHANGED").execute();
    check(repository.findAll().one().value()).is("bbb");
  }

  @Test
  void manyValues() {
    repository.insert(generator.get().withId("id1").withValue("val1").withOptional("opt1").withNullable("null1"));
    repository.insert(generator.get().withId("id2").withValue("val2").withOptional("opt2").withNullable("null2"));
    repository.update(stringHolder)
            .set(stringHolder.value, "a")
            .set(stringHolder.optional, Optional.of("b"))
            .set(stringHolder.nullable, "c")
            .execute();

    TypeHolder.StringHolder hol1 = repository.findAll().fetch().get(0);
    check(hol1.value()).is("a");
    check(hol1.optional()).is(Optional.of("b"));
    check(hol1.nullable()).is("c");

    TypeHolder.StringHolder hol2 = repository.findAll().fetch().get(1);
    check(hol2.value()).is("a");
    check(hol2.optional()).is(Optional.of("b"));
    check(hol2.nullable()).is("c");
  }

  @Test
  void manyValuesWithFilter() {
    repository.insert(generator.get().withId("id1").withValue("val1").withOptional("opt1"));
    repository.insert(generator.get().withId("id2").withValue("val2").withOptional("opt2"));

    repository.update(stringHolder.id.is("id1"))
            .set(stringHolder.value, "a1")
            .set(stringHolder.optional, Optional.of("b1"))
            .set(stringHolder.nullable, "c1")
            .execute();

    repository.update(stringHolder.id.is("id2"))
            .set(stringHolder.value, "a2")
            .set(stringHolder.optional, Optional.of("b2"))
            .set(stringHolder.nullable, "c2")
            .execute();

    TypeHolder.StringHolder hol1 = repository.find(stringHolder.id.is("id1")).one();
    check(hol1.value()).is("a1");
    check(hol1.optional()).is(Optional.of("b1"));
    check(hol1.nullable()).is("c1");

    TypeHolder.StringHolder hol2 = repository.find(stringHolder.id.is("id2")).one();
    check(hol2.value()).is("a2");
    check(hol2.optional()).is(Optional.of("b2"));
    check(hol2.nullable()).is("c2");
  }

  @Test
  void nullsAndOptionals() {
    repository.insert(generator.get().withId("id1").withValue("val1").withOptional("opt1").withNullable("null1"));

    repository.update(stringHolder.id.is("id1"))
            .set(stringHolder.optional, Optional.empty())
            .set(stringHolder.nullable, null)
            .execute();

    TypeHolder.StringHolder hol1 = repository.find(stringHolder.id.is("id1")).one();
    check(hol1.value()).is("val1");
    check(hol1.optional()).is(Optional.empty());
    check(hol1.nullable()).isNull();
  }
}
