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

package org.immutables.fixture;

import org.immutables.check.Checkers;
import org.junit.jupiter.api.Test;

class LazyHashTest {

  @Test
  void lazyHash() {
    ImmutableLazyHash h1 = ImmutableLazyHash.builder().s("a").i(1).b(true).build();
    ImmutableLazyHash h2 = ImmutableLazyHash.builder().s("b").i(2).b(false).build();

    Checkers.check(h1.hashCode()).not().is(0); // ensure hashcode is computed
    Checkers.check(h1.hashCode()).not().is(h2.hashCode());
    Checkers.check(h1.equals(ImmutableLazyHash.builder().s("a").i(1).b(true).build()));
    Checkers.check(!h1.equals(h2));
  }
}
