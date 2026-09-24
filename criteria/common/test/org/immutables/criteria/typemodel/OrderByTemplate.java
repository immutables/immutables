/*
 * Copyright 2026 Immutables Authors and Contributors
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

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.immutables.check.Checkers.check;

/**
 * Ordering: by one key, by several, and by an attribute which is absent on some of the stored
 * values.
 *
 * <p>An absent value has to be given a position rather than dereferenced. The position asserted
 * here -- first when ascending, last when descending -- is the one the backends this template is
 * registered for happen to agree on, not a rule that holds everywhere: elasticsearch places an
 * absent value last in both directions, postgres treats null as the largest value and mysql as
 * the smallest. So this is a default, and a caller who needs the other one currently cannot ask
 * for it.
 */
public abstract class OrderByTemplate {

  private final StringHolderRepository repository;
  private final StringHolderCriteria string = StringHolderCriteria.stringHolder;
  private final Supplier<ImmutableStringHolder> generator;

  protected OrderByTemplate(Backend backend) {
    this.repository = new StringHolderRepository(backend);
    this.generator = TypeHolder.StringHolder.generator();
  }

  @Test
  void ascending() {
    repository.insert(generator.get().withId("id1").withValue("c"));
    repository.insert(generator.get().withId("id2").withValue("a"));
    repository.insert(generator.get().withId("id3").withValue("b"));

    check(ids(repository.findAll().orderBy(string.value.asc()).fetch())).isOf("id2", "id3", "id1");
  }

  @Test
  void descending() {
    repository.insert(generator.get().withId("id1").withValue("c"));
    repository.insert(generator.get().withId("id2").withValue("a"));
    repository.insert(generator.get().withId("id3").withValue("b"));

    check(ids(repository.findAll().orderBy(string.value.desc()).fetch())).isOf("id1", "id3", "id2");
  }

  /**
   * A nullable attribute is a first-class question when filtering ({@code isAbsent()}), so
   * ordering by one has to work as well.
   */
  @Test
  void nullableAscending() {
    insertNullableFixture();

    check(ids(repository.findAll().orderBy(string.nullable.asc()).fetch())).isOf("id2", "id1", "id3");
  }

  @Test
  void nullableDescending() {
    insertNullableFixture();

    check(ids(repository.findAll().orderBy(string.nullable.desc()).fetch())).isOf("id3", "id1", "id2");
  }

  /**
   * The absent value is reached only through a tie on the first key, so a sort key with no null in
   * it at all is enough to have to order one.
   */
  @Test
  void severalKeys() {
    insertNullableFixture();

    check(ids(repository.findAll().orderBy(string.value.asc(), string.nullable.asc()).fetch()))
            .isOf("id2", "id1", "id3");
  }

  private void insertNullableFixture() {
    repository.insert(generator.get().withId("id1").withValue("same").withNullable("a"));
    repository.insert(generator.get().withId("id2").withValue("same").withNullable(null));
    repository.insert(generator.get().withId("id3").withValue("same").withNullable("b"));
  }

  private static List<String> ids(List<TypeHolder.StringHolder> holders) {
    return holders.stream().map(TypeHolder.StringHolder::id).collect(Collectors.toList());
  }
}
