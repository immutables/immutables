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

package org.immutables.criteria;

import org.immutables.criteria.personmodel.PersonCriteria;
import org.immutables.criteria.expression.DebugExpressionVisitor;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Tests that expression is built correctly by "serializing" it to string
 */
public class ExpressionAsStringTest {

  @Test
  public void string() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.create();

    assertExpressional(crit.bestFriend.isPresent(), "call op=IS_PRESENT path=bestFriend");
    assertExpressional(crit.bestFriend.isAbsent(), "call op=IS_ABSENT path=bestFriend");
    assertExpressional(crit.bestFriend.value().nickName.isEqualTo("aa"), "call op=EQUAL path=bestFriend.nickName constant=aa");
//    assertExpressional(crit.bestFriend.value(f -> f.nickName.isEqualTo("bbb")), "call op=EQUAL path=bestFriend.nickName constant=bbb");
    assertExpressional(crit.fullName.isIn("n1", "n2"), "call op=IN path=fullName constant=[n1, n2]");

    assertExpressional(crit.fullName.isEqualTo("John").or().fullName.isEqualTo("Marry"),
            "call op=OR",
                    "  call op=EQUAL path=fullName constant=John",
                    "  call op=EQUAL path=fullName constant=Marry");

    assertExpressional(crit.bestFriend.value().nickName.isEqualTo("John"),
                    "call op=EQUAL path=bestFriend.nickName constant=John");

  }

  @Test
  public void not() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.create();

    assertExpressional(crit.fullName.not(n -> n.isEqualTo("John")),
            "call op=NOT",
                    "  call op=EQUAL path=fullName constant=John");

    assertExpressional(crit.not(f -> f.fullName.isEqualTo("John").bestFriend.isPresent()),
            "call op=NOT",
                    "  call op=AND",
                    "    call op=EQUAL path=fullName constant=John",
                    "    call op=IS_PRESENT path=bestFriend");

  }

  private static void assertExpressional(DocumentCriteria<?> crit, String ... expectedLines) {
    final StringWriter out = new StringWriter();
    Criterias.toFilterExpression(crit).accept(new DebugExpressionVisitor<>(new PrintWriter(out)));
    final String expected = Arrays.stream(expectedLines).collect(Collectors.joining(System.lineSeparator()));
    Assert.assertEquals(expected, out.toString().trim());
  }

}
