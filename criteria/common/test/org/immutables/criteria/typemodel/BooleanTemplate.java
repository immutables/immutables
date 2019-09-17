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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

/**
 * Testing predicates, projections, sorting etc. on booleans
 */
public abstract class BooleanTemplate {

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
    ids(holder.value.isFalse()).isEmpty();
    ids(holder.value.isTrue()).isEmpty();
    ids(holder.value.is(true)).isEmpty();
    ids(holder.value.is(true)).isEmpty();
    ids(holder.value.is(false)).isEmpty();
    ids(holder.nullable.isTrue()).isEmpty();
    ids(holder.nullable.isFalse()).isEmpty();
    ids(holder.optional.isTrue()).isEmpty();
    ids(holder.optional.isFalse()).isEmpty();
  }

  @Test
  void basic() {
    repository.insert(generator.get().withId("id1").withValue(true));
    repository.insert(generator.get().withId("id2").withValue(false));

    ids(holder.value.is(true)).hasContentInAnyOrder("id1");
    ids(holder.value.isTrue()).hasContentInAnyOrder("id1");
    ids(holder.value.is(false)).hasContentInAnyOrder("id2");
    ids(holder.value.isFalse()).hasContentInAnyOrder("id2");
  }

  @Test
  protected void optional() {
    repository.insert(generator.get().withId("id1").withValue(true).withOptional(false));
    repository.insert(generator.get().withId("id2").withValue(false).withOptional(true));
    repository.insert(generator.get().withId("id3").withValue(false).withOptional(Optional.empty()));

    ids(holder.optional.isPresent()).hasContentInAnyOrder("id1", "id2");
    ids(holder.optional.isAbsent()).hasContentInAnyOrder("id3");
    ids(holder.optional.is(true)).hasContentInAnyOrder("id2");
    ids(holder.optional.isTrue()).hasContentInAnyOrder("id2");
    ids(holder.optional.is(false)).hasContentInAnyOrder("id1");
    ids(holder.optional.isFalse()).hasContentInAnyOrder("id1");
  }

  @Test
  void nullable() {
    repository.insert(generator.get().withId("id1").withValue(true).withNullable(false));
    repository.insert(generator.get().withId("id2").withValue(false).withNullable(true));
    repository.insert(generator.get().withId("id3").withValue(false).withNullable(null));

    ids(holder.nullable.isPresent()).hasContentInAnyOrder("id1", "id2");
    ids(holder.nullable.isAbsent()).hasContentInAnyOrder("id3");
    ids(holder.nullable.is(true)).hasContentInAnyOrder("id2");
    ids(holder.nullable.isTrue()).hasContentInAnyOrder("id2");
    ids(holder.nullable.is(false)).hasContentInAnyOrder("id1");
    ids(holder.nullable.isFalse()).hasContentInAnyOrder("id1");
  }

  @Test
  void projection() {
    repository.insert(generator.get().withId("id1").withValue(true).withNullable(false).withBoxed(Boolean.TRUE).withOptional(Optional.of(false)));
    repository.insert(generator.get().withId("id2").withValue(false).withNullable(true).withBoxed(Boolean.FALSE).withOptional(Optional.of(true)));
    repository.insert(generator.get().withId("id3").withValue(false).withNullable(null).withBoxed(Boolean.TRUE).withOptional(Optional.empty()));

    // projection of one attribute
    check(repository.findAll().select(holder.id).fetch()).hasContentInAnyOrder("id1", "id2", "id3");
    check(repository.findAll().select(holder.value).fetch()).hasContentInAnyOrder(true, false, false);
    check(repository.findAll().select(holder.nullable).asOptional().fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of(true), Optional.of(false));
    check(repository.findAll().select(holder.boxed).fetch()).hasContentInAnyOrder(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE);
    check(repository.findAll().select(holder.optional).fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of(true), Optional.of(false));

    // 2 attributes using tuple
    check(repository.findAll().select(holder.nullable, holder.optional)
            .map(tuple -> String.format("nullable=%s optional=%s", tuple.get(holder.nullable), tuple.get(holder.optional).map(Objects::toString).orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("nullable=false optional=false", "nullable=true optional=true", "nullable=null optional=<empty>");
    // 2 attributes using mapper
    check(repository.findAll().select(holder.nullable, holder.optional)
            .map((nullable, optional) -> String.format("nullable=%s optional=%s", nullable, optional.map(Objects::toString).orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("nullable=false optional=false", "nullable=true optional=true", "nullable=null optional=<empty>");

    // 3 attributes using tuple
    check(repository.findAll().select(holder.id, holder.nullable, holder.optional)
            .map(tuple -> String.format("id=%s nullable=%s optional=%s", tuple.get(holder.id), tuple.get(holder.nullable), tuple.get(holder.optional).map(Objects::toString).orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("id=id1 nullable=false optional=false", "id=id2 nullable=true optional=true", "id=id3 nullable=null optional=<empty>");
    // 3 attributes using mapper
    check(repository.findAll().select(holder.id, holder.nullable, holder.optional)
            .map((id, nullable, optional) -> String.format("id=%s nullable=%s optional=%s", id, nullable, optional.map(Objects::toString).orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("id=id1 nullable=false optional=false", "id=id2 nullable=true optional=true", "id=id3 nullable=null optional=<empty>");

    // 4 attributes using mapper
    check(repository.findAll().select(holder.id, holder.value, holder.nullable, holder.optional)
            .map((id, value, nullable, optional) -> String.format("id=%s value=%s nullable=%s optional=%s", id, value, nullable, optional.map(Objects::toString).orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("id=id1 value=true nullable=false optional=false", "id=id2 value=false nullable=true optional=true", "id=id3 value=false nullable=null optional=<empty>");
  }

  private IterableChecker<List<String>, String> ids(BooleanHolderCriteria criteria) {
    return CriteriaChecker.<TypeHolder.BooleanHolder>of(repository.find(criteria)).toList(TypeHolder.BooleanHolder::id);
  }

}
