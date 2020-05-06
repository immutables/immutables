/*
 * Copyright 2020 Immutables Authors and Contributors
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

package org.immutables.criteria.expression;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.immutables.check.Checkers.check;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpressionsTest {

  @Test
  void and() {
    assertThrows(IllegalArgumentException.class, () -> Expressions.and(Collections.emptyList()));
    assertThrows(IllegalArgumentException.class, () -> Expressions.and(Collections.singleton(Constant.of(true))));

    Call c1 = Expressions.and(Constant.of(true), Constant.of(false));
    Call c2 = Expressions.and(Arrays.asList(Constant.of(true), Constant.of(false)));

    for (Call c: Arrays.asList(c1, c2)) {
      check(c.operator()).is(Operators.AND);
      check(c.arguments()).isOf(Constant.of(true), Constant.of(false));
    }
  }

  @Test
  void or() {
    assertThrows(IllegalArgumentException.class, () -> Expressions.or(Collections.emptyList()));
    assertThrows(IllegalArgumentException.class, () -> Expressions.or(Collections.singleton(Constant.of(true))));

    Call c1 = Expressions.or(Constant.of(true), Constant.of(false));
    Call c2 = Expressions.or(Arrays.asList(Constant.of(true), Constant.of(false)));

    for (Call c: Arrays.asList(c1, c2)) {
      check(c.operator()).is(Operators.OR);
      check(c.arguments()).isOf(Constant.of(true), Constant.of(false));
    }
  }
}