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

package org.immutables.criteria.matcher;

import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.jupiter.api.Test;

import static org.immutables.check.Checkers.check;

class MatchersTest {

  @Test
  void projection() {
    Expression expr1 = Matchers.toExpression(PersonCriteria.person.fullName);
    check(expr1 instanceof Path);
    check(((Path) expr1).toStringPath()).is("fullName");

    Expression expr2 = Matchers.toExpression(PersonCriteria.person.bestFriend.value().hobby);
    check(expr2 instanceof Path);
    check(((Path) expr2).toStringPath()).is("bestFriend.hobby");
  }


}