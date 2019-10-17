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

import org.immutables.criteria.Criterias;
import org.immutables.criteria.backend.Backend;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Test things like {@code A and (B or C)}, {@code A or B and C} {@code A or (not B and C)} etc.
 * Make sure boolean conditions are correctly grouped (in terms of expression priority).
 */
public abstract class BooleanLogicTemplate {

  private final StringHolderRepository repository;
  private final StringHolderCriteria string = StringHolderCriteria.stringHolder;
  private final Supplier<ImmutableStringHolder> generator;

  protected BooleanLogicTemplate(Backend backend) {
    this.repository = new StringHolderRepository(backend);
    this.generator = TypeHolder.StringHolder.generator();
  }

  // in general A.or().B.and(C)  !=  A.or().B.C
  // (A or B) and C != A or B and C

  @Test
  void booleanLogic() {
    repository.insert(generator.get().withId("id").withValue("value").withNullable("nullable").withOptional(Optional.of("optional")));

    // values are same as attribute names so boolean logic is easier to follow
    // value == 'value' or id == 'id' (true)
    // nullable == 'no' (false)

    // true and (false or false) == false
    logicalFalse(string.value.is("value").and(string.nullable.is("no").or().id.is("no")));

    // true and (false or true) == true
    logicalTrue(string.value.is("value").and(string.nullable.is("no").or().id.is("id")));

    // false and (true or true) == false
    logicalFalse(string.value.is("no").and(string.nullable.is("nullable").or().id.is("id")));

    // false or (true and true) == true
    logicalTrue(string.value.is("no").or(string.nullable.is("nullable").id.is("id")));

    // false or (true and false) == false
    logicalFalse(string.value.is("no").or(string.nullable.is("nullable").id.is("no")));

    // (false or true) and false == false
    logicalFalse(string.value.is("no").or().nullable.is("nullable").and(string.id.is("no")));

    // false or true or false == true
    logicalTrue(string.value.is("no").or().nullable.is("nullable").or().id.is("no"));

    // false or false or false == false
    logicalFalse(string.value.is("no").or().nullable.is("no").or().id.is("no"));

    // false and false or true == true
    logicalTrue(string.value.is("no").nullable.is("no").or().id.is("id"));
  }

  @Test
  void notLogic() {
    repository.insert(generator.get().withId("id").withValue("value").withNullable("nullable").withOptional(Optional.of("optional")));

    // not false == true
    logicalTrue(string.value.not(v -> v.is("no")));

    // not true == false
    logicalFalse(string.value.not(v -> v.is("value")));

    // not false and not false == true
    logicalTrue(string.value.not(v -> v.is("no")).value.not(v -> v.is("no")));

    // not true and not false == false
    logicalFalse(string.value.not(v -> v.is("value")).value.not(v -> v.is("no")));

    // not true and not true == false
    logicalFalse(string.value.not(v -> v.is("value")).value.not(v -> v.is("value")));

    // not true or not true == false
    logicalFalse(string.value.not(v -> v.is("value")).or().value.not(v -> v.is("value")));

    // not true or not false == true
    logicalTrue(string.value.not(v -> v.is("value")).or().value.not(v -> v.is("no")));
  }

  /**
   * Expect single result to be returned (one document matched)
   */
  private void logicalTrue(StringHolderCriteria criteria) {
    logical(criteria, 1);
  }

  /**
   * Expect empty result set (no documents matched)
   */
  private void logicalFalse(StringHolderCriteria criteria) {
    logical(criteria, 0);
  }

  private void logical(StringHolderCriteria criteria, int expected) {
    String query = Criterias.toQuery(criteria).filter().map(Objects::toString)
            .orElseThrow(() -> new IllegalStateException("Filter should be present"));
    List<?> result = repository.find(criteria).fetch();
    if (result.size() != expected) {
      Assertions.fail(String.format("Expected filter %n%s%n to return %d result(s) but got %d", query, expected, result.size()));
    }
  }

}
