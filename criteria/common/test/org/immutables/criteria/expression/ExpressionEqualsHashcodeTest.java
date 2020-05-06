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

import com.google.common.testing.EqualsTester;
import org.immutables.criteria.personmodel.Person;
import org.junit.jupiter.api.Test;

@SuppressWarnings("UnstableApiUsage")
class ExpressionEqualsHashcodeTest {

  @Test
  void constant() {
    // null
    new EqualsTester()
            .addEqualityGroup(Expressions.constantOfType(null, String.class), Constant.ofType(null, String.class))
            .addEqualityGroup(Constant.ofType(null, Boolean.class))
            .testEquals();

    // string
    new EqualsTester()
            .addEqualityGroup(Constant.of("a"), Constant.of("a"))
            .addEqualityGroup(Constant.of(""))
            .testEquals();
    // int
    new EqualsTester()
            .addEqualityGroup(Constant.of(1), Constant.of(1))
            .addEqualityGroup(Constant.of(2))
            .testEquals();

    // boolean
    new EqualsTester()
            .addEqualityGroup(Constant.of(false), Constant.of(false))
            .addEqualityGroup(Constant.of(true))
            .testEquals();
  }

  @Test
  void path() throws Exception {
    Path id1 = Path.ofMember(Person.class.getDeclaredMethod("id"));
    Path id2 = Path.ofMember(Person.class.getDeclaredMethod("id"));
    Path fullName = Path.ofMember(Person.class.getDeclaredMethod("fullName"));

    new EqualsTester()
            .addEqualityGroup(id1, id2)
            .addEqualityGroup(fullName)
            .testEquals();
  }

  @Test
  void call() throws Exception {
    Path id1 = Path.ofMember(Person.class.getDeclaredMethod("id"));
    Path id2 = Path.ofMember(Person.class.getDeclaredMethod("id"));

    Expression e1 = Expressions.call(Operators.EQUAL, id1, Expressions.constant("1"));
    Expression e2 = Expressions.call(Operators.EQUAL, id2, Expressions.constant("1"));
    Expression e3 = Expressions.call(Operators.EQUAL, id1, Expressions.constant("2"));

    Path fullName = Path.ofMember(Person.class.getDeclaredMethod("fullName"));
    Expression e4 = Expressions.call(Operators.EQUAL, fullName, Expressions.constant("1"));

    new EqualsTester()
            .addEqualityGroup(e1, e2)
            .addEqualityGroup(e3)
            .addEqualityGroup(e4)
            .testEquals();
  }
}
