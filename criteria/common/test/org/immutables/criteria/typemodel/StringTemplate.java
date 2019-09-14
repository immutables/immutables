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
import java.util.function.Supplier;

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
    values(repository.find(string.value.startsWith("a"))).isEmpty();
    values(repository.find(string.value.startsWith(""))).isEmpty();

    repository.insert(generator.get().withValue("a"));
    repository.insert(generator.get().withValue("aa"));
    repository.insert(generator.get().withValue("b"));
    repository.insert(generator.get().withValue("bb"));

    values(repository.find(string.value.startsWith("a"))).hasContentInAnyOrder("a", "aa");
    values(repository.find(string.value.startsWith("b"))).hasContentInAnyOrder("b", "bb");
    values(repository.find(string.value.startsWith("c"))).isEmpty();
    values(repository.find(string.value.startsWith(""))).hasContentInAnyOrder("a", "aa", "b", "bb");
  }

  @Test
  void equality() {
    values(repository.find(string.value.is(""))).isEmpty();
    values(repository.find(string.value.isNot(""))).isEmpty();
    repository.insert(generator.get().withValue("a"));
    repository.insert(generator.get().withValue("bb"));
    repository.insert(generator.get().withValue("ccc"));

    values(repository.find(string.value.is("a"))).hasContentInAnyOrder("a");
    values(repository.find(string.value.is("bb"))).hasContentInAnyOrder("bb");
    values(repository.find(string.value.isNot("bb"))).hasContentInAnyOrder("a", "ccc");
    values(repository.find(string.value.isNot("a"))).hasContentInAnyOrder("bb", "ccc");
    values(repository.find(string.value.in("a", "bb", "ccc"))).hasContentInAnyOrder("a", "bb", "ccc");
    values(repository.find(string.value.in("a", "bb"))).hasContentInAnyOrder("a", "bb");
    values(repository.find(string.value.notIn("a", "bb", "ccc"))).isEmpty();
  }

  @Test
  void whitespace() {
    repository.insert(generator.get().withValue(""));
    repository.insert(generator.get().withValue(" "));
    repository.insert(generator.get().withValue("\n"));

    values(repository.find(string.value.is(""))).hasContentInAnyOrder("");
    values(repository.find(string.value.is(" "))).hasContentInAnyOrder(" ");
    values(repository.find(string.value.is("\n"))).hasContentInAnyOrder("\n");
    values(repository.find(string.value.isNot(""))).hasContentInAnyOrder(" ", "\n");
    values(repository.find(string.value.isNot(" "))).hasContentInAnyOrder("", "\n");
    values(repository.find(string.value.isNot("\n"))).hasContentInAnyOrder(" ", "");
  }

  @Test
  void endsWith() {
    values(repository.find(string.value.endsWith("a"))).isEmpty();
    values(repository.find(string.value.endsWith(""))).isEmpty();

    repository.insert(generator.get().withValue("a"));
    repository.insert(generator.get().withValue("aa"));
    repository.insert(generator.get().withValue("b"));
    repository.insert(generator.get().withValue("bb"));

    values(repository.find(string.value.endsWith("a"))).hasContentInAnyOrder("a", "aa");
    values(repository.find(string.value.endsWith("b"))).hasContentInAnyOrder("b", "bb");
    values(repository.find(string.value.endsWith("c"))).isEmpty();
    values(repository.find(string.value.endsWith(""))).hasContentInAnyOrder("a", "aa", "b", "bb");
  }

  @Test
  void contains() {
    values(repository.find(string.value.contains(""))).isEmpty();
    values(repository.find(string.value.contains(" "))).isEmpty();
    values(repository.find(string.value.contains("aa"))).isEmpty();
    repository.insert(generator.get().withValue("a"));
    values(repository.find(string.value.contains("a"))).hasContentInAnyOrder("a");
    values(repository.find(string.value.contains("b"))).isEmpty();
    values(repository.find(string.value.contains(""))).hasContentInAnyOrder("a");
    repository.insert(generator.get().withValue("b"));
    values(repository.find(string.value.contains("a"))).hasContentInAnyOrder("a");
    values(repository.find(string.value.contains("b"))).hasContentInAnyOrder("b");

    repository.insert(generator.get().withValue("ab"));
    values(repository.find(string.value.contains("a"))).hasContentInAnyOrder("a", "ab");
    values(repository.find(string.value.contains("b"))).hasContentInAnyOrder("b", "ab");
    values(repository.find(string.value.contains("ab"))).hasContentInAnyOrder("ab");
    values(repository.find(string.value.contains("ba"))).isEmpty();
    values(repository.find(string.value.contains("abc"))).isEmpty();
  }

  @Test
  void nullable() {
    repository.insert(generator.get().withValue("null").withNullable(null));
    repository.insert(generator.get().withValue("notnull").withNullable("notnull"));

    values(repository.find(string.nullable.isPresent())).hasContentInAnyOrder("notnull");
    values(repository.find(string.nullable.isAbsent())).hasContentInAnyOrder("null");
    values(repository.find(string.nullable.is("null"))).isEmpty();
    values(repository.find(string.nullable.is(""))).isEmpty();
    values(repository.find(string.value.is("null"))).hasContentInAnyOrder("null");
    values(repository.find(string.value.is("notnull"))).hasContentInAnyOrder("notnull");
  }

  private static IterableChecker<List<String>, String> values(SyncReader<TypeHolder.StringHolder> reader) {
    return CriteriaChecker.<TypeHolder.StringHolder>of(reader).toList(TypeHolder.StringHolder::value);
  }

}
