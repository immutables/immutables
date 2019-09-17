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

package org.immutables.criteria.repository.reactive;

import io.reactivex.Flowable;
import org.immutables.criteria.backend.NonUniqueResultException;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.repository.FakeBackend;
import org.immutables.criteria.typemodel.TypeHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.immutables.check.Checkers.check;

/**
 * Check that correct exceptions are thrown by {@link ReactiveFetcher}
 */
class ReactiveFetcherDelegateTest {

  @Test
  void fetch() {
    check(Flowable.fromPublisher(create().fetch()).toList().blockingGet()).isEmpty();
    check(Flowable.fromPublisher(create("one").fetch()).toList().blockingGet()).isOf("one");
    check(Flowable.fromPublisher(create("one", "two").fetch()).toList().blockingGet()).isOf("one", "two");
    check(Flowable.fromPublisher(create("one", "two", "three").fetch()).toList().blockingGet()).isOf("one", "two", "three");
  }

  @Test
  void one() {
    Assertions.assertThrows(NonUniqueResultException.class, () -> {
      // empty
      Flowable.fromPublisher(create().one()).toList().blockingGet();
    });

    // one
    check(Flowable.fromPublisher(create("one").one()).toList().blockingGet()).hasContentInAnyOrder("one");

    Assertions.assertThrows(NonUniqueResultException.class, () -> {
      // two
      Flowable.fromPublisher(create("one", "two").one()).toList().blockingGet();
    });
  }

  @Test
  void oneOrNone() {
    check(Flowable.fromPublisher(create().oneOrNone()).toList().blockingGet()).isEmpty();
    check(Flowable.fromPublisher(create("one").oneOrNone()).toList().blockingGet()).hasContentInAnyOrder("one");
    Assertions.assertThrows(NonUniqueResultException.class, () -> {
      // two
      Flowable.fromPublisher(create("one", "two").one()).toList().blockingGet();
    });
  }

  @Test
  void exists() {
    check(Flowable.fromPublisher(create().exists()).toList().blockingGet()).isOf(false);
    check(Flowable.fromPublisher(create("one").exists()).toList().blockingGet()).isOf(true);
    check(Flowable.fromPublisher(create("one", "two").exists()).toList().blockingGet()).isOf(true);
    check(Flowable.fromPublisher(create("one", "two", "three").exists()).toList().blockingGet()).isOf(true);
  }

  private static ReactiveFetcher<String> create(String ... values) {
    FakeBackend backend = new FakeBackend(Flowable.fromArray(values));
    return ReactiveFetcherDelegate.of(Query.of(TypeHolder.StringHolder.class), backend.open(TypeHolder.StringHolder.class));
  }
}
