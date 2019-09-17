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

import org.immutables.check.Checkers;
import org.immutables.check.IterableChecker;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.NonUniqueResultException;
import org.immutables.criteria.personmodel.CriteriaChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

/**
 * Testing various string operations prefix/suffix/length etc.
 */
public abstract class StringTemplate {

  private final StringHolderRepository repository;
  private final StringHolderCriteria string = StringHolderCriteria.stringHolder;
  private final Supplier<ImmutableStringHolder> generator;

  protected StringTemplate(Backend backend) {
    this.repository = new StringHolderRepository(backend);
    this.generator = TypeHolder.StringHolder.generator();
  }

  @Test
  protected void startsWith() {
    ids(string.value.startsWith("a")).isEmpty();
    ids(string.value.startsWith("")).isEmpty();

    repository.insert(generator.get().withValue("a"));
    repository.insert(generator.get().withValue("aa"));
    repository.insert(generator.get().withValue("b"));
    repository.insert(generator.get().withValue("bb"));

    ids(string.value.startsWith("a")).hasContentInAnyOrder("a", "aa");
    ids(string.value.startsWith("b")).hasContentInAnyOrder("b", "bb");
    ids(string.value.startsWith("c")).isEmpty();
    ids(string.value.startsWith("")).hasContentInAnyOrder("a", "aa", "b", "bb");
  }

  @Test
  protected void equality() {
    ids(string.value.is("")).isEmpty();
    ids(string.value.isNot("")).isEmpty();
    repository.insert(generator.get().withValue("a"));
    repository.insert(generator.get().withValue("bb"));
    repository.insert(generator.get().withValue("ccc"));

    ids(string.value.is("a")).hasContentInAnyOrder("a");
    ids(string.value.is("bb")).hasContentInAnyOrder("bb");
    ids(string.value.isNot("bb")).hasContentInAnyOrder("a", "ccc");
    ids(string.value.isNot("a")).hasContentInAnyOrder("bb", "ccc");
    ids(string.value.in("a", "bb", "ccc")).hasContentInAnyOrder("a", "bb", "ccc");
    ids(string.value.in("a", "bb")).hasContentInAnyOrder("a", "bb");
    ids(string.value.notIn("a", "bb", "ccc")).isEmpty();
  }

  @Test
  protected void whitespace() {
    repository.insert(generator.get().withValue(""));
    repository.insert(generator.get().withValue(" "));
    repository.insert(generator.get().withValue("\n"));

    ids(string.value.is("")).hasContentInAnyOrder("");
    ids(string.value.is(" ")).hasContentInAnyOrder(" ");
    ids(string.value.is("\n")).hasContentInAnyOrder("\n");
    ids(string.value.isNot("")).hasContentInAnyOrder(" ", "\n");
    ids(string.value.isNot(" ")).hasContentInAnyOrder("", "\n");
    ids(string.value.isNot("\n")).hasContentInAnyOrder(" ", "");
  }

  @Test
  protected void endsWith() {
    ids(string.value.endsWith("a")).isEmpty();
    ids(string.value.endsWith("")).isEmpty();

    repository.insert(generator.get().withValue("a"));
    repository.insert(generator.get().withValue("aa"));
    repository.insert(generator.get().withValue("b"));
    repository.insert(generator.get().withValue("bb"));

    ids(string.value.endsWith("a")).hasContentInAnyOrder("a", "aa");
    ids(string.value.endsWith("b")).hasContentInAnyOrder("b", "bb");
    ids(string.value.endsWith("c")).isEmpty();
    ids(string.value.endsWith("")).hasContentInAnyOrder("a", "aa", "b", "bb");
  }

  @Test
  protected void contains() {
    ids(string.value.contains("")).isEmpty();
    ids(string.value.contains(" ")).isEmpty();
    ids(string.value.contains("aa")).isEmpty();
    repository.insert(generator.get().withValue("a"));
    ids(string.value.contains("a")).hasContentInAnyOrder("a");
    ids(string.value.contains("b")).isEmpty();
    ids(string.value.contains("")).hasContentInAnyOrder("a");
    repository.insert(generator.get().withValue("b"));
    ids(string.value.contains("a")).hasContentInAnyOrder("a");
    ids(string.value.contains("b")).hasContentInAnyOrder("b");

    repository.insert(generator.get().withValue("ab"));
    ids(string.value.contains("a")).hasContentInAnyOrder("a", "ab");
    ids(string.value.contains("b")).hasContentInAnyOrder("b", "ab");
    ids(string.value.contains("ab")).hasContentInAnyOrder("ab");
    ids(string.value.contains("ba")).isEmpty();
    ids(string.value.contains("abc")).isEmpty();
  }

  @Test
  protected void empty() {
    ids(string.value.isEmpty()).isEmpty();
    ids(string.value.notEmpty()).isEmpty();

    repository.insert(generator.get().withValue("a"));
    ids(string.value.isEmpty()).isEmpty();
    ids(string.value.notEmpty()).hasContentInAnyOrder("a");

    repository.insert(generator.get().withValue(""));
    ids(string.value.isEmpty()).hasContentInAnyOrder("");
    ids(string.value.notEmpty()).hasContentInAnyOrder("a");

    repository.insert(generator.get().withValue(" "));
    ids(string.value.isEmpty()).hasContentInAnyOrder("");
    ids(string.value.notEmpty()).hasContentInAnyOrder("a", " ");

    repository.insert(generator.get().withValue("\n"));
    ids(string.value.isEmpty()).hasContentInAnyOrder("");
    ids(string.value.notEmpty()).hasContentInAnyOrder("a", " ", "\n");

    repository.insert(generator.get().withValue(""));
    ids(string.value.isEmpty()).hasContentInAnyOrder("", "");
  }

  @Test
  protected void projection() {
    repository.insert(generator.get().withId("id1").withValue("null").withNullable(null).withOptional(Optional.empty()));
    repository.insert(generator.get().withId("id2").withValue("notnull").withNullable("NOT_NULL").withOptional(Optional.of("NOT_NULL2")));

    // projection of one attribute
    check(repository.findAll().select(string.id).fetch()).hasContentInAnyOrder("id1", "id2");
    check(repository.findAll().select(string.value).fetch()).hasContentInAnyOrder("null", "notnull");
    check(repository.findAll().select(string.nullable).asOptional().fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of("NOT_NULL"));
    check(repository.findAll().select(string.optional).fetch()).hasContentInAnyOrder(Optional.empty(), Optional.of("NOT_NULL2"));


    // 2 attributes using tuple
    check(repository.findAll().select(string.nullable, string.optional)
            .map(tuple -> String.format("nullable=%s optional=%s", tuple.get(string.nullable), tuple.get(string.optional).orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("nullable=null optional=<empty>", "nullable=NOT_NULL optional=NOT_NULL2");
    // 2 attributes using mapper
    check(repository.findAll().select(string.nullable, string.optional)
            .map((nullable, optional) -> String.format("nullable=%s optional=%s", nullable, optional.orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("nullable=null optional=<empty>", "nullable=NOT_NULL optional=NOT_NULL2");

    // 3 attributes using tuple
    check(repository.findAll().select(string.id, string.nullable, string.optional)
            .map(tuple -> String.format("id=%s nullable=%s optional=%s", tuple.get(string.id), tuple.get(string.nullable), tuple.get(string.optional).orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("id=id1 nullable=null optional=<empty>", "id=id2 nullable=NOT_NULL optional=NOT_NULL2");
    // 3 attributes using mapper
    check(repository.findAll().select(string.id, string.nullable, string.optional)
            .map((id, nullable, optional) -> String.format("id=%s nullable=%s optional=%s", id, nullable, optional.orElse("<empty>"))).fetch())
            .hasContentInAnyOrder("id=id1 nullable=null optional=<empty>", "id=id2 nullable=NOT_NULL optional=NOT_NULL2");

  }

  @Test
  protected void nullable() {
    repository.insert(generator.get().withValue("null").withNullable(null));
    repository.insert(generator.get().withValue("notnull").withNullable("notnull"));

    ids(string.nullable.isPresent()).hasContentInAnyOrder("notnull");
    ids(string.nullable.isAbsent()).hasContentInAnyOrder("null");
    ids(string.nullable.is("null")).isEmpty();
    ids(string.nullable.is("")).isEmpty();
    ids(string.value.is("null")).hasContentInAnyOrder("null");
    ids(string.value.is("notnull")).hasContentInAnyOrder("notnull");
  }

  @Test
  protected void optional() {
    repository.insert(generator.get().withValue("null").withNullable(null).withOptional(Optional.empty()));
    repository.insert(generator.get().withValue("notnull").withNullable("notnull").withOptional("notempty"));

    ids(string.optional.isAbsent()).hasContentInAnyOrder("null");
    ids(string.optional.isPresent()).hasContentInAnyOrder("notnull");
    ids(string.optional.is("null")).isEmpty();
    ids(string.optional.is("notempty")).hasContentInAnyOrder("notnull");
    ids(string.optional.is("")).isEmpty();
  }

  /**
   * validate {@code one() / oneOrNone() / exists()} methods
   */
  @Test
  void fetch() {
    repository.insert(generator.get().withValue("v1"));
    repository.insert(generator.get().withValue("v1"));
    repository.insert(generator.get().withValue("v2"));

    // exists
    Checkers.check(repository.findAll().exists());
    Checkers.check(repository.find(string.value.is("v1")).exists());
    Checkers.check(repository.find(string.value.is("v2")).exists());
    Checkers.check(!repository.find(string.value.is("v3")).exists());

    // one
    Assertions.assertThrows(NonUniqueResultException.class, () -> repository.findAll().one());
    Assertions.assertThrows(NonUniqueResultException.class, () -> repository.find(string.value.is("v1")).one());
    Assertions.assertThrows(NonUniqueResultException.class, () -> repository.find(string.value.is("v1")).one());
    Assertions.assertThrows(NonUniqueResultException.class, () -> repository.find(string.value.is("v3")).one());
    check(repository.find(string.value.is("v2")).one().value()).is("v2");

    // oneOrNone
    Assertions.assertThrows(NonUniqueResultException.class, () -> repository.findAll().oneOrNone());
    Assertions.assertThrows(NonUniqueResultException.class, () -> repository.find(string.value.is("v1")).oneOrNone());
    check(repository.find(string.value.is("v2")).oneOrNone().get().value()).is("v2");
    check(!repository.find(string.value.is("v3")).oneOrNone().isPresent());
  }

  private IterableChecker<List<String>, String> ids(StringHolderCriteria criteria) {
    return  CriteriaChecker.<TypeHolder.StringHolder>of(repository.find(criteria)).toList(TypeHolder.StringHolder::value);
  }
}
