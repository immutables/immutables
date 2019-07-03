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

import org.immutables.criteria.expression.DebugExpressionVisitor;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.Assert;
import org.junit.Ignore;
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
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;

    assertExpressional(crit.bestFriend.isPresent(), "call op=IS_PRESENT path=bestFriend");
    assertExpressional(crit.bestFriend.isAbsent(), "call op=IS_ABSENT path=bestFriend");
    assertExpressional(crit.bestFriend.value().hobby.isEqualTo("ski"), "call op=EQUAL path=bestFriend.hobby constant=ski");
//    assertExpressional(crit.bestFriend.value(f -> f.nickName.isEqualTo("bbb")), "call op=EQUAL path=bestFriend.nickName constant=bbb");
    assertExpressional(crit.fullName.isIn("n1", "n2"), "call op=IN path=fullName constant=[n1, n2]");

    assertExpressional(crit.fullName.isEqualTo("John").or().fullName.isEqualTo("Marry"),
            "call op=OR",
                    "  call op=EQUAL path=fullName constant=John",
                    "  call op=EQUAL path=fullName constant=Marry");

    assertExpressional(crit.bestFriend.value().hobby.isEqualTo("ski"),
                    "call op=EQUAL path=bestFriend.hobby constant=ski");
    assertExpressional(crit.bestFriend.value(f -> f.hobby.isEqualTo("hiking")) ,
            "call op=EQUAL path=bestFriend.hobby constant=hiking");
  }

  @Test
  public void not() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;

    assertExpressional(crit.fullName.not(n -> n.isEqualTo("John")),
            "call op=NOT",
                    "  call op=EQUAL path=fullName constant=John");

    assertExpressional(crit.not(f -> f.fullName.isEqualTo("John").bestFriend.isPresent()),
            "call op=NOT",
                    "  call op=AND",
                    "    call op=EQUAL path=fullName constant=John",
                    "    call op=IS_PRESENT path=bestFriend");
  }

  @Test
  public void and() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;

    PersonCriteria<PersonCriteria.Self> other = PersonCriteria.person
            .and(crit.age.isAtMost(1)).and(crit.age.isAtLeast(2));

    assertExpressional(other, "call op=AND",
            "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
            "  call op=GREATER_THAN_OR_EQUAL path=age constant=2");

    assertExpressional(other.and(crit.age.isEqualTo(3)),
            "call op=AND",
                    "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
                    "  call op=GREATER_THAN_OR_EQUAL path=age constant=2",
                    "  call op=EQUAL path=age constant=3");

    assertExpressional(other.and(crit.age.isEqualTo(3)),
            "call op=AND",
                    "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
                    "  call op=GREATER_THAN_OR_EQUAL path=age constant=2",
                    "  call op=EQUAL path=age constant=3");
  }

  @Test
  public void or() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;

    PersonCriteria<PersonCriteria.Self> other = PersonCriteria.person
            .or(crit.age.isAtMost(1)).or(crit.age.isAtLeast(2));

    assertExpressional(other, "call op=OR",
            "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
            "  call op=GREATER_THAN_OR_EQUAL path=age constant=2");

    assertExpressional(other.or(crit.age.isEqualTo(3)),
            "call op=OR",
                    "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
                    "  call op=GREATER_THAN_OR_EQUAL path=age constant=2",
                    "  call op=EQUAL path=age constant=3");

    assertExpressional(crit.or(crit.age.isAtMost(1)).or(crit.age.isAtLeast(2))
                    .or(crit.age.isEqualTo(3)),
            "call op=OR",
            "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
            "  call op=GREATER_THAN_OR_EQUAL path=age constant=2",
            "  call op=EQUAL path=age constant=3");
  }

  @Test
  public void next() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;
    assertExpressional(crit.bestFriend.value().hobby.isEqualTo("ski"), "call op=EQUAL path=bestFriend.hobby constant=ski");
    assertExpressional(crit.address.value().zip.isEqualTo("12345"),
            "call op=EQUAL path=address.zip constant=12345");
    assertExpressional(PersonCriteria.person
                    .address.value().zip.isEqualTo("12345")
                    .or()
                    .bestFriend.value().hobby.isEqualTo("ski"),
            "call op=OR",
            "  call op=EQUAL path=address.zip constant=12345",
            "  call op=EQUAL path=bestFriend.hobby constant=ski");
  }

  @Test
  public void inner() {
    assertExpressional(PersonCriteria.person.bestFriend.value(f -> f.hobby.isEqualTo("hiking")),
            "call op=EQUAL path=bestFriend.hobby constant=hiking");

    assertExpressional(PersonCriteria.person.bestFriend.value(f -> f.not(v -> v.hobby.isEqualTo("hiking"))),
            "call op=NOT",
            "  call op=EQUAL path=bestFriend.hobby constant=hiking");
  }

  @Ignore
  @Test
  public void debug() {
    assertExpressional(PersonCriteria.person.bestFriend
                    .value(f -> f.hobby.isEqualTo("hiking").hobby.isEqualTo("ski")),
            "call op=AND",
            "  call op=EQUAL path=bestFriend.hobby constant=hiking",
            "  call op=EQUAL path=bestFriend.hobby constant=ski");
  }

  private static void assertExpressional(Criterion<?> crit, String ... expectedLines) {
    final StringWriter out = new StringWriter();
    Query query = Criterias.toQuery(crit);
    query.filter().ifPresent(f -> f.accept(new DebugExpressionVisitor<>(new PrintWriter(out))));
    final String expected = Arrays.stream(expectedLines).collect(Collectors.joining(System.lineSeparator()));
    Assert.assertEquals(expected, out.toString().trim());
  }

}
