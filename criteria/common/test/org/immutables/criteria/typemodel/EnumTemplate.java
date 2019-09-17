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

public abstract class EnumTemplate {

  private final EnumHolderRepository repository;
  private final EnumHolderCriteria criteria;
  private final Supplier<ImmutableEnumHolder> generator;

  protected EnumTemplate(Backend backend) {
    this.repository = new EnumHolderRepository(backend);
    this.criteria = EnumHolderCriteria.enumHolder;
    this.generator = TypeHolder.EnumHolder.generator();
  }

  @Test
  void equality() {
    repository.insert(generator.get().withId("id1").withValue(TypeHolder.Foo.ONE));
    repository.insert(generator.get().withId("id2").withValue(TypeHolder.Foo.TWO));
    repository.insert(generator.get().withId("id3").withValue(TypeHolder.Foo.THREE));

    ids(criteria.value.is(TypeHolder.Foo.ONE)).isOf("id1");
    ids(criteria.value.is(TypeHolder.Foo.TWO)).isOf("id2");
    ids(criteria.value.isNot(TypeHolder.Foo.TWO)).hasContentInAnyOrder("id1", "id3");
    ids(criteria.value.in(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO)).hasContentInAnyOrder("id1", "id2");
    ids(criteria.value.notIn(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO)).hasContentInAnyOrder("id3");
    ids(criteria.value.notIn(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO, TypeHolder.Foo.THREE)).isEmpty();
  }

  @Test
  void projection() {
    repository.insert(generator.get().withId("id1").withValue(TypeHolder.Foo.ONE).withNullable(TypeHolder.Foo.ONE).withOptional(Optional.of(TypeHolder.Foo.ONE)));
    repository.insert(generator.get().withId("id2").withValue(TypeHolder.Foo.TWO).withNullable(null).withOptional(Optional.empty()));
    repository.insert(generator.get().withId("id3").withValue(TypeHolder.Foo.THREE).withNullable(null).withOptional(Optional.of(TypeHolder.Foo.THREE)));

    // projection of one attribute
    check(repository.findAll().select(criteria.id).fetch()).hasContentInAnyOrder("id1", "id2", "id3");
    check(repository.findAll().select(criteria.value).fetch()).hasContentInAnyOrder(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO, TypeHolder.Foo.THREE);
    check(repository.findAll().select(criteria.nullable).asOptional().fetch()).hasContentInAnyOrder(Optional.empty(), Optional.empty(), Optional.of(TypeHolder.Foo.ONE));
    check(repository.findAll().select(criteria.optional).fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of(TypeHolder.Foo.ONE), Optional.of(TypeHolder.Foo.THREE));

    // 2 attributes using tuple
    check(repository.findAll().select(criteria.nullable, criteria.optional)
            .map(tuple -> String.format("nullable=%s optional=%s", tuple.get(criteria.nullable), tuple.get(criteria.optional).map(Objects::toString).orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("nullable=ONE optional=ONE", "nullable=null optional=<empty>", "nullable=null optional=THREE");
    // 2 attributes using mapper
    check(repository.findAll().select(criteria.nullable, criteria.optional)
            .map((nullable, optional) -> String.format("nullable=%s optional=%s", nullable, optional.map(Objects::toString).orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("nullable=ONE optional=ONE", "nullable=null optional=<empty>", "nullable=null optional=THREE");

    // 3 attributes using tuple
    check(repository.findAll().select(criteria.id, criteria.nullable, criteria.optional)
            .map(tuple -> String.format("id=%s nullable=%s optional=%s", tuple.get(criteria.id), tuple.get(criteria.nullable), tuple.get(criteria.optional).map(Objects::toString).orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("id=id1 nullable=ONE optional=ONE", "id=id2 nullable=null optional=<empty>", "id=id3 nullable=null optional=THREE");
    // 3 attributes using mapper
    check(repository.findAll().select(criteria.id, criteria.nullable, criteria.optional)
            .map((id, nullable, optional) -> String.format("id=%s nullable=%s optional=%s", id, nullable, optional.map(Objects::toString).orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("id=id1 nullable=ONE optional=ONE", "id=id2 nullable=null optional=<empty>", "id=id3 nullable=null optional=THREE");

    // 4 attributes using mapper
    check(repository.findAll().select(criteria.id, criteria.value, criteria.nullable, criteria.optional)
            .map((id, value, nullable, optional) -> String.format("id=%s value=%s nullable=%s optional=%s", id, value, nullable, optional.map(Objects::toString).orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("id=id1 value=ONE nullable=ONE optional=ONE", "id=id2 value=TWO nullable=null optional=<empty>", "id=id3 value=THREE nullable=null optional=THREE");
    
  }

  private IterableChecker<List<String>, String> ids(EnumHolderCriteria criteria) {
    return  CriteriaChecker.<TypeHolder.EnumHolder>of(repository.find(criteria)).toList(TypeHolder.EnumHolder::id);
  }

}
