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
    values(string.value.startsWith("a")).isEmpty();
    values(string.value.startsWith("")).isEmpty();

    repository.insert(generator.get().withValue("a"));
    repository.insert(generator.get().withValue("aa"));
    repository.insert(generator.get().withValue("b"));
    repository.insert(generator.get().withValue("bb"));

    values(string.value.startsWith("a")).hasContentInAnyOrder("a", "aa");
    values(string.value.startsWith("b")).hasContentInAnyOrder("b", "bb");
    values(string.value.startsWith("c")).isEmpty();
    values(string.value.startsWith("")).hasContentInAnyOrder("a", "aa", "b", "bb");
  }

  @Test
  protected void equality() {
    values(string.value.is("")).isEmpty();
    values(string.value.isNot("")).isEmpty();
    repository.insert(generator.get().withValue("a"));
    repository.insert(generator.get().withValue("bb"));
    repository.insert(generator.get().withValue("ccc"));

    values(string.value.is("a")).hasContentInAnyOrder("a");
    values(string.value.is("bb")).hasContentInAnyOrder("bb");
    values(string.value.isNot("bb")).hasContentInAnyOrder("a", "ccc");
    values(string.value.isNot("a")).hasContentInAnyOrder("bb", "ccc");
    values(string.value.in("a", "bb", "ccc")).hasContentInAnyOrder("a", "bb", "ccc");
    values(string.value.in("a", "bb")).hasContentInAnyOrder("a", "bb");
    values(string.value.notIn("a", "bb", "ccc")).isEmpty();
  }

  @Test
  protected void whitespace() {
    repository.insert(generator.get().withValue(""));
    repository.insert(generator.get().withValue(" "));
    repository.insert(generator.get().withValue("\n"));

    values(string.value.is("")).hasContentInAnyOrder("");
    values(string.value.is(" ")).hasContentInAnyOrder(" ");
    values(string.value.is("\n")).hasContentInAnyOrder("\n");
    values(string.value.isNot("")).hasContentInAnyOrder(" ", "\n");
    values(string.value.isNot(" ")).hasContentInAnyOrder("", "\n");
    values(string.value.isNot("\n")).hasContentInAnyOrder(" ", "");
  }

  @Test
  protected void endsWith() {
    values(string.value.endsWith("a")).isEmpty();
    values(string.value.endsWith("")).isEmpty();

    repository.insert(generator.get().withValue("a"));
    repository.insert(generator.get().withValue("aa"));
    repository.insert(generator.get().withValue("b"));
    repository.insert(generator.get().withValue("bb"));

    values(string.value.endsWith("a")).hasContentInAnyOrder("a", "aa");
    values(string.value.endsWith("b")).hasContentInAnyOrder("b", "bb");
    values(string.value.endsWith("c")).isEmpty();
    values(string.value.endsWith("")).hasContentInAnyOrder("a", "aa", "b", "bb");
  }

  @Test
  protected void contains() {
    values(string.value.contains("")).isEmpty();
    values(string.value.contains(" ")).isEmpty();
    values(string.value.contains("aa")).isEmpty();
    repository.insert(generator.get().withValue("a"));
    values(string.value.contains("a")).hasContentInAnyOrder("a");
    values(string.value.contains("b")).isEmpty();
    values(string.value.contains("")).hasContentInAnyOrder("a");
    repository.insert(generator.get().withValue("b"));
    values(string.value.contains("a")).hasContentInAnyOrder("a");
    values(string.value.contains("b")).hasContentInAnyOrder("b");

    repository.insert(generator.get().withValue("ab"));
    values(string.value.contains("a")).hasContentInAnyOrder("a", "ab");
    values(string.value.contains("b")).hasContentInAnyOrder("b", "ab");
    values(string.value.contains("ab")).hasContentInAnyOrder("ab");
    values(string.value.contains("ba")).isEmpty();
    values(string.value.contains("abc")).isEmpty();
  }

  @Test
  protected void empty() {
    values(string.value.isEmpty()).isEmpty();
    values(string.value.notEmpty()).isEmpty();

    repository.insert(generator.get().withValue("a"));
    values(string.value.isEmpty()).isEmpty();
    values(string.value.notEmpty()).hasContentInAnyOrder("a");

    repository.insert(generator.get().withValue(""));
    values(string.value.isEmpty()).hasContentInAnyOrder("");
    values(string.value.notEmpty()).hasContentInAnyOrder("a");

    repository.insert(generator.get().withValue(" "));
    values(string.value.isEmpty()).hasContentInAnyOrder("");
    values(string.value.notEmpty()).hasContentInAnyOrder("a", " ");

    repository.insert(generator.get().withValue("\n"));
    values(string.value.isEmpty()).hasContentInAnyOrder("");
    values(string.value.notEmpty()).hasContentInAnyOrder("a", " ", "\n");

    repository.insert(generator.get().withValue(""));
    values(string.value.isEmpty()).hasContentInAnyOrder("", "");
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

    values(string.nullable.isPresent()).hasContentInAnyOrder("notnull");
    values(string.nullable.isAbsent()).hasContentInAnyOrder("null");
    values(string.nullable.is("null")).isEmpty();
    values(string.nullable.is("")).isEmpty();
    values(string.value.is("null")).hasContentInAnyOrder("null");
    values(string.value.is("notnull")).hasContentInAnyOrder("notnull");
  }

  @Test
  protected void optional() {
    repository.insert(generator.get().withValue("null").withNullable(null).withOptional(Optional.empty()));
    repository.insert(generator.get().withValue("notnull").withNullable("notnull").withOptional("notempty"));

    values(string.optional.isAbsent()).hasContentInAnyOrder("null");
    values(string.optional.isPresent()).hasContentInAnyOrder("notnull");
    values(string.optional.is("null")).isEmpty();
    values(string.optional.is("notempty")).hasContentInAnyOrder("notnull");
    values(string.optional.is("")).isEmpty();
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

  /**
   * Usually single-quotes have to be replaced
   */
  @Test
  void quoteEscape() {
    repository.insert(generator.get().withId("id1").withValue("O'Hare"));
    repository.insert(generator.get().withId("id2").withValue("'"));
    repository.insert(generator.get().withId("id3").withValue("''"));
    repository.insert(generator.get().withId("id4").withValue("'test'"));

    ids(string.value.is("O'Hare")).isOf("id1");
    ids(string.value.is("'")).isOf("id2");
    ids(string.value.is("''")).isOf("id3");
    ids(string.value.is("'test'")).isOf("id4");
    ids(string.value.is("'''")).isEmpty();
    ids(string.value.in("'", "''")).hasContentInAnyOrder("id2", "id3");
    ids(string.value.in("O'Hare", "'test'")).hasContentInAnyOrder("id1", "id4");
  }

  /**
   * Usually double-quotes have to be replaced
   */
  @Test
  void doubleQuoteEscape() {
    repository.insert(generator.get().withId("id1").withValue("\""));
    repository.insert(generator.get().withId("id2").withValue("\"\""));
    repository.insert(generator.get().withId("id3").withValue("\"\"\""));
    repository.insert(generator.get().withId("id4").withValue("\"test\""));
    repository.insert(generator.get().withId("id5").withValue("a\"b"));

    ids(string.value.is("\"")).isOf("id1");
    ids(string.value.is("\"\"")).isOf("id2");
    ids(string.value.is("\"\"\"")).isOf("id3");
    ids(string.value.is("\"test\"")).isOf("id4");
    ids(string.value.is("a\"b")).isOf("id5");
    ids(string.value.is("\" \"")).isEmpty();
    ids(string.value.in("\"", "\"\"")).hasContentInAnyOrder("id1", "id2");
  }

  @Test
  void specialChars() {
    repository.insert(generator.get().withId("id1").withValue("!@#$%^&*()_+"));
    repository.insert(generator.get().withId("id2").withValue("[]{};',./"));
    repository.insert(generator.get().withId("id3").withValue("`~"));
    repository.insert(generator.get().withId("id4").withValue("<>|\\"));

    ids(string.value.is("!@#$%^&*()_+")).isOf("id1");
    ids(string.value.is("[]{};',./")).isOf("id2");
    ids(string.value.is("`~")).isOf("id3");
    ids(string.value.is("<>|\\")).isOf("id4");
  }

  /**
   * Return {@link TypeHolder.StringHolder#value()} after applying a criteria
   */
  private IterableChecker<List<String>, String> values(StringHolderCriteria criteria) {
    return  CriteriaChecker.<TypeHolder.StringHolder>ofReader(repository.find(criteria)).toList(TypeHolder.StringHolder::value);
  }

  /**
   * Return {@link TypeHolder.StringHolder#id()} after applying a criteria
   */
  private IterableChecker<List<String>, String> ids(StringHolderCriteria criteria) {
    return  CriteriaChecker.<TypeHolder.StringHolder>ofReader(repository.find(criteria)).toList(TypeHolder.StringHolder::id);
  }
}
