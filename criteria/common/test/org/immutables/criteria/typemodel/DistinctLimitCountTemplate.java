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

import java.util.Optional;
import java.util.function.Supplier;

import static org.immutables.check.Checkers.check;

/**
 * Testing functionality of {@code DISTINCT} along with {@code LIMIT} and perhaps {@code COUNT(*)}.
 * Currently {@code distinct()} is available only after projections:
 * <pre>
 *   {@code
 *      repository.findAll().select(person.name).distinct().limit(2).fetch();
 *   }
 * </pre>
 */
public abstract class DistinctLimitCountTemplate {

  private final StringHolderRepository repository;
  private final StringHolderCriteria string = StringHolderCriteria.stringHolder;
  private final Supplier<ImmutableStringHolder> generator;

  protected DistinctLimitCountTemplate(Backend backend) {
    this.repository = new StringHolderRepository(backend);
    this.generator = TypeHolder.StringHolder.generator();
  }

  @Test
  void empty() {
    check(repository.findAll().select(string.value).distinct().fetch()).isEmpty();
    check(repository.findAll().select(string.value).distinct().oneOrNone()).is(Optional.empty());
    check(repository.findAll().select(string.value).distinct().limit(1).fetch()).isEmpty();
    check(repository.findAll().select(string.value).distinct().limit(1).oneOrNone()).is(Optional.empty());
    check(repository.findAll().select(string.value).distinct().count()).is(0L);
    check(repository.findAll().select(string.value, string.nullable).distinct().map((x, y) -> "bad").fetch()).isEmpty();
    check(repository.findAll().select(string.value, string.nullable).distinct().map((x, y) -> "bad").count()).is(0L);
  }

  /**
   * Similar to {@code SELECT DISTINCT foo from myTable LIMIT 1}
   */
  @Test
  void singleProjection() {
    repository.insert(generator.get().withValue("v1"));
    repository.insert(generator.get().withValue("v1"));
    repository.insert(generator.get().withValue("v2"));
    repository.insert(generator.get().withValue("v2"));
    repository.insert(generator.get().withValue("v3"));

    check(repository.findAll().select(string.value).distinct().fetch()).hasContentInAnyOrder("v1", "v2", "v3");
    check(repository.findAll().select(string.value).distinct().limit(1).fetch()).hasSize(1);
    check(repository.findAll().select(string.value).distinct().limit(2).fetch()).hasSize(2);
    check(repository.findAll().select(string.value).distinct().limit(3).fetch()).hasContentInAnyOrder("v1", "v2", "v3");
    check(repository.findAll().select(string.value).distinct().limit(4).fetch()).hasContentInAnyOrder("v1", "v2", "v3");
    check(repository.findAll().select(string.value).distinct().limit(5).fetch()).hasContentInAnyOrder("v1", "v2", "v3");

    // with filters
    check(repository.find(string.value.is("v1")).select(string.value).distinct().fetch()).hasContentInAnyOrder("v1");
    check(repository.find(string.value.in("v3", "v2")).select(string.value).distinct().fetch()).hasContentInAnyOrder("v2", "v3");
    check(repository.find(string.value.is("BAD")).select(string.value).distinct().fetch()).isEmpty();
    check(repository.find(string.value.in("v1", "BAD")).select(string.value).distinct().fetch()).hasContentInAnyOrder("v1");

    // with count/limit/filter
    check(repository.findAll().select(string.value).distinct().count()).is(3L);
    check(repository.findAll().select(string.value).distinct().limit(1).count()).is(1L);
    check(repository.findAll().select(string.value).distinct().limit(2).count()).is(2L);
    check(repository.findAll().select(string.value).distinct().limit(3).count()).is(3L);
    check(repository.findAll().select(string.value).distinct().limit(4).count()).is(3L);
    check(repository.findAll().select(string.value).distinct().limit(5).count()).is(3L);
    check(repository.find(string.value.is("BAD")).select(string.value).distinct().count()).is(0L);
    check(repository.find(string.value.is("v1")).select(string.value).distinct().count()).is(1L);
    check(repository.find(string.value.in("v1", "v2")).select(string.value).distinct().count()).is(2L);
  }

  /**
   * Similar to {@code SELECT DISTINCT a, b from myTable LIMIT 1}
   */
  @Test
  void doubleProjection() {
    repository.insert(generator.get().withValue("v1").withNullable("n1"));
    repository.insert(generator.get().withValue("v1").withNullable("n1"));
    repository.insert(generator.get().withValue("v2").withNullable("n2"));
    repository.insert(generator.get().withValue("v2").withNullable("n2"));
    repository.insert(generator.get().withValue("v3").withNullable("n3"));

    check(repository.findAll().select(string.value, string.nullable)
            .distinct()
            .map((x, y) -> x + "-" +y)
            .fetch()).hasContentInAnyOrder("v1-n1", "v2-n2", "v3-n3");

    check(repository.findAll().select(string.value, string.nullable)
            .distinct()
            .limit(1)
            .map((x, y) -> x + "-" +y)
            .fetch()).hasSize(1);

    // add a filter
    check(repository.find(string.value.in("v1", "v3"))
            .select(string.value, string.nullable)
            .distinct()
            .map((x, y) -> x + "-" +y)
            .fetch()).hasContentInAnyOrder("v1-n1", "v3-n3");

  }
}
