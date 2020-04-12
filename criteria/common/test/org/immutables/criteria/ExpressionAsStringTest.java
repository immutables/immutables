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
import org.immutables.criteria.matcher.StringMatcher;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Tests that expression is built correctly by "serializing" it to string.
 * It is easier to debug expression when it is presented as human-readable tree
 */
class ExpressionAsStringTest {

  private final PersonCriteria person = PersonCriteria.person;

  @Test
  void string() {
    assertExpressional(person.bestFriend.isPresent(), "call op=IS_PRESENT path=bestFriend");
    assertExpressional(person.bestFriend.isAbsent(), "call op=IS_ABSENT path=bestFriend");

    assertExpressional(person.bestFriend.value().hobby.is("ski"),
            "call op=EQUAL path=bestFriend.hobby constant=ski");
    assertExpressional(person.fullName.in("n1", "n2"), "call op=IN path=fullName constant=[n1, n2]");

    assertExpressional(person.fullName.is("John").or().fullName.is("Mary"),
            "call op=OR",
                    "  call op=EQUAL path=fullName constant=John",
                    "  call op=EQUAL path=fullName constant=Mary");

    assertExpressional(person.fullName.is("John").or().fullName.is("Mary").or().fullName.is("Bob"),
            "call op=OR",
                    "  call op=EQUAL path=fullName constant=John",
                    "  call op=EQUAL path=fullName constant=Mary",
                    "  call op=EQUAL path=fullName constant=Bob");

    assertExpressional(person.bestFriend.value().hobby.is("ski"),
                    "call op=EQUAL path=bestFriend.hobby constant=ski");
  }

  @Test
  void with() {
    assertExpressional(person.with(c -> c.fullName.is("John")),
            "call op=EQUAL path=fullName constant=John");
    assertExpressional(person.with(c -> c.fullName.is("John").nickName.isPresent()),
            "call op=AND",
            "  call op=EQUAL path=fullName constant=John",
            "  call op=IS_PRESENT path=nickName");
    assertExpressional(person.fullName.with(f -> f.is("John")),
            "call op=EQUAL path=fullName constant=John");

    assertExpressional(person.bestFriend.value().with(f -> f.hobby.is("aaa")),
            "call op=EQUAL path=bestFriend.hobby constant=aaa");

    assertExpressional(person.bestFriend.value().hobby.with(h -> h.is("a").is("b")),
            "call op=AND",
            "  call op=EQUAL path=bestFriend.hobby constant=a",
            "  call op=EQUAL path=bestFriend.hobby constant=b");

    assertExpressional(person.bestFriend.value().hobby.with(h -> h.is("a").is("b")),
            "call op=AND",
            "  call op=EQUAL path=bestFriend.hobby constant=a",
            "  call op=EQUAL path=bestFriend.hobby constant=b");

    assertExpressional(person.bestFriend.value().hobby.with(h -> h.is("a").is("b"))
                   .fullName.isEmpty(),
            "call op=AND",
            "  call op=EQUAL path=bestFriend.hobby constant=a",
            "  call op=EQUAL path=bestFriend.hobby constant=b",
            "  call op=EQUAL path=fullName constant=");

    assertExpressional(PersonCriteria.person
                    .fullName.with(StringMatcher::isEmpty)
                    .or()
                    .fullName.with(StringMatcher::notEmpty),
            "call op=OR",
            "  call op=EQUAL path=fullName constant=",
            "  call op=NOT_EQUAL path=fullName constant="
    );

    assertExpressional(PersonCriteria.person
                    .fullName.with(StringMatcher::isEmpty)
                    .fullName.with(StringMatcher::notEmpty),
            "call op=AND",
            "  call op=EQUAL path=fullName constant=",
            "  call op=NOT_EQUAL path=fullName constant="
    );

    assertExpressional(PersonCriteria.person.age.atLeast(21).with(p -> p.isActive.isTrue()),
            "call op=AND",
            "  call op=GREATER_THAN_OR_EQUAL path=age constant=21",
            "  call op=EQUAL path=isActive constant=true");

    assertExpressional(PersonCriteria.person.with(p -> p.isActive.isTrue()).with(p -> p.fullName.is("a")),
            "call op=AND",
            "  call op=EQUAL path=isActive constant=true",
            "  call op=EQUAL path=fullName constant=a");
  }

  @Test
  void with2() {
    // nested
    assertExpressional(person.with(p1 -> p1.with(p2-> p2.fullName.is("a"))),
            "call op=EQUAL path=fullName constant=a");

    assertExpressional(PersonCriteria.person.with(p1 -> p1.with(p2-> p2.fullName.is("a")).age.is(11)),
            "call op=AND",
            "  call op=EQUAL path=fullName constant=a",
            "  call op=EQUAL path=age constant=11");

    assertExpressional(PersonCriteria.person.age.is(11).with(p1 -> p1.with(p2-> p2.nickName.is("a"))),
            "call op=AND",
            "  call op=EQUAL path=age constant=11",
            "  call op=EQUAL path=nickName constant=a"
            );

    assertExpressional(PersonCriteria.person.bestFriend.value().hobby.is("aaa")
                    .with(p1 -> p1.age.is(22).with(p2-> p2.bestFriend.value().hobby.is("bbb"))),
            "call op=AND",
            "  call op=EQUAL path=bestFriend.hobby constant=aaa",
            "  call op=AND",
            "    call op=EQUAL path=age constant=22",
            "    call op=EQUAL path=bestFriend.hobby constant=bbb"
            );

    assertExpressional(PersonCriteria.person.with(p1 -> p1.with(p2-> p2.with(p3 -> p3.fullName.is("a")))),
            "call op=EQUAL path=fullName constant=a");

  }

  @Test
  void not() {
    assertExpressional(person.fullName.not(n -> n.is("John")),
            "call op=NOT",
                    "  call op=EQUAL path=fullName constant=John");

    assertExpressional(person.not(f -> f.fullName.is("John").bestFriend.isPresent()),
            "call op=NOT",
                    "  call op=AND",
                    "    call op=EQUAL path=fullName constant=John",
                    "    call op=IS_PRESENT path=bestFriend");

    assertExpressional(PersonCriteria.person.age.atLeast(21).not(p -> p.isActive.isTrue()),
            "call op=AND",
            "  call op=GREATER_THAN_OR_EQUAL path=age constant=21",
            "  call op=NOT",
            "    call op=EQUAL path=isActive constant=true");
  }

  @Test
  void debug() {
    assertExpressional(PersonCriteria.person.bestFriend.value().hobby.with(h -> h.is("a").is("b")),
            "call op=AND",
            "  call op=EQUAL path=bestFriend.hobby constant=a",
            "  call op=EQUAL path=bestFriend.hobby constant=b");
  }

  @Test
  void and() {
    PersonCriteria other = PersonCriteria.person
            .and(person.age.atMost(1)).and(person.age.atLeast(2));

    assertExpressional(other, "call op=AND",
            "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
            "  call op=GREATER_THAN_OR_EQUAL path=age constant=2");

    assertExpressional(other.and(person.age.is(3)),
            "call op=AND",
                    "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
                    "  call op=GREATER_THAN_OR_EQUAL path=age constant=2",
                    "  call op=EQUAL path=age constant=3");

    assertExpressional(other.and(person.age.is(3)),
            "call op=AND",
                    "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
                    "  call op=GREATER_THAN_OR_EQUAL path=age constant=2",
                    "  call op=EQUAL path=age constant=3");
  }

  @Test
  void or() {
    PersonCriteria other = person
            .or(person.age.atMost(1)).or(person.age.atLeast(2));

    assertExpressional(other, "call op=OR",
            "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
            "  call op=GREATER_THAN_OR_EQUAL path=age constant=2");

    assertExpressional(other.or(person.age.is(3)),
            "call op=OR",
                    "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
                    "  call op=GREATER_THAN_OR_EQUAL path=age constant=2",
                    "  call op=EQUAL path=age constant=3");

    assertExpressional(person.or(person.age.atMost(1)).or(person.age.atLeast(2))
                    .or(person.age.is(3)),
            "call op=OR",
            "  call op=LESS_THAN_OR_EQUAL path=age constant=1",
            "  call op=GREATER_THAN_OR_EQUAL path=age constant=2",
            "  call op=EQUAL path=age constant=3");

  }

  /**
   * AND / ORs combined like {@code (A and B) or (C and D)}
   */
  @Test
  void andOrCombined() {
    // A or (B and C)
    assertExpressional(person.age.is(1).or()
                    .age.is(2)
                    .age.is(3),
            "call op=OR",
            "  call op=EQUAL path=age constant=1",
            "  call op=AND",
            "    call op=EQUAL path=age constant=2",
            "    call op=EQUAL path=age constant=3");

    // (A and B) or C
    assertExpressional(person.age.is(1)
                    .age.is(2)
                    .or()
                    .age.is(3),
            "call op=OR",
            "  call op=AND",
            "    call op=EQUAL path=age constant=1",
            "    call op=EQUAL path=age constant=2",
            "  call op=EQUAL path=age constant=3");

    // A or (B and C and D)
    assertExpressional(person.age.is(1).or()
                    .age.is(2)
                    .age.is(3)
                    .age.is(4),
            "call op=OR",
            "  call op=EQUAL path=age constant=1",
            "  call op=AND",
            "    call op=EQUAL path=age constant=2",
            "    call op=EQUAL path=age constant=3",
            "    call op=EQUAL path=age constant=4");

    // (A and B) or (C and D)
    assertExpressional(person.age.is(1)
                    .age.is(2)
            .or()
                    .age.is(3)
                    .age.is(4),
            "call op=OR",
            "  call op=AND",
            "    call op=EQUAL path=age constant=1",
            "    call op=EQUAL path=age constant=2",
            "  call op=AND",
            "    call op=EQUAL path=age constant=3",
            "    call op=EQUAL path=age constant=4");

    // (A and B and C) or D
    assertExpressional(person.age.is(1)
                    .age.is(2)
                    .age.is(3)
            .or()
                    .age.is(4),
            "call op=OR",
            "  call op=AND",
            "    call op=EQUAL path=age constant=1",
            "    call op=EQUAL path=age constant=2",
            "    call op=EQUAL path=age constant=3",
            "  call op=EQUAL path=age constant=4");

  }

  @Test
  void andOrAsCriteriaParams() {

    assertExpressional(person.age.is(1)
                    .and(person.age.is(2)),
            "call op=AND",
            "  call op=EQUAL path=age constant=1",
            "  call op=EQUAL path=age constant=2"
    );

    assertExpressional(person.age.is(1)
                    .and(person.age.is(2))
                    .and(person.age.is(3)),
    "call op=AND",
            "  call op=EQUAL path=age constant=1",
            "  call op=EQUAL path=age constant=2",
            "  call op=EQUAL path=age constant=3"
    );

    // combination of OR/AND
    assertExpressional(person.age.is(1).or(person.age.is(2).age.is(3)),
            "call op=OR",
            "  call op=EQUAL path=age constant=1",
            "  call op=AND",
            "    call op=EQUAL path=age constant=2",
            "    call op=EQUAL path=age constant=3");

    assertExpressional(person.age.is(1).or(person.age.is(2).age.is(3)),
            "call op=OR",
            "  call op=EQUAL path=age constant=1",
            "  call op=AND",
            "    call op=EQUAL path=age constant=2",
            "    call op=EQUAL path=age constant=3");

    assertExpressional(person.age.is(1).and(person.age.is(2).or().age.is(3)),
            "call op=AND",
            "  call op=EQUAL path=age constant=1",
            "  call op=OR",
            "    call op=EQUAL path=age constant=2",
            "    call op=EQUAL path=age constant=3");

    assertExpressional(person.age.is(1).and(person.age.is(2).or().age.is(3)),
            "call op=AND",
            "  call op=EQUAL path=age constant=1",
            "  call op=OR",
            "    call op=EQUAL path=age constant=2",
            "    call op=EQUAL path=age constant=3");

    assertExpressional(person.age.is(1).or().age.is(2).and(person.age.is(3)),
            "call op=AND",
            "  call op=OR",
            "    call op=EQUAL path=age constant=1",
            "    call op=EQUAL path=age constant=2",
            "  call op=EQUAL path=age constant=3");

  }

  @Test
  void next() {
    assertExpressional(person.bestFriend.value().hobby.is("ski"), "call op=EQUAL path=bestFriend.hobby constant=ski");

    assertExpressional(person.bestFriend.value().hobby.is("ski")
                    .age.is(22),
            "call op=AND",
            "  call op=EQUAL path=bestFriend.hobby constant=ski",
            "  call op=EQUAL path=age constant=22");

    assertExpressional(person.address.value().zip.is("12345"),
            "call op=EQUAL path=address.zip constant=12345");
    assertExpressional(PersonCriteria.person
                    .address.value().zip.is("12345")
                    .or()
                    .bestFriend.value().hobby.is("ski"),
            "call op=OR",
            "  call op=EQUAL path=address.zip constant=12345",
            "  call op=EQUAL path=bestFriend.hobby constant=ski");
  }

  @Test
  void between() {
    assertExpressional(person.age.between(1, 2).or().age.is(3),
            "call op=OR",
            "  call op=AND",
            "    call op=GREATER_THAN_OR_EQUAL path=age constant=1",
            "    call op=LESS_THAN_OR_EQUAL path=age constant=2",
            "  call op=EQUAL path=age constant=3"
    );

    assertExpressional(person.age.is(3).or().age.between(1, 2),
            "call op=OR",
            "  call op=EQUAL path=age constant=3",
            "  call op=AND",
            "    call op=GREATER_THAN_OR_EQUAL path=age constant=1",
            "    call op=LESS_THAN_OR_EQUAL path=age constant=2"
    );

    assertExpressional(person.age.is(1).age.between(2, 3),
            "call op=AND",
            "  call op=EQUAL path=age constant=1",
            "  call op=AND",
            "    call op=GREATER_THAN_OR_EQUAL path=age constant=2",
            "    call op=LESS_THAN_OR_EQUAL path=age constant=3"
    );
  }

  @Test
  void inner() {
    assertExpressional(person.bestFriend.value().with(f -> f.hobby.is("hiking")),
            "call op=EQUAL path=bestFriend.hobby constant=hiking");

    assertExpressional(person.bestFriend.value().with(f -> f.not(v -> v.hobby.is("hiking"))),
            "call op=NOT",
            "  call op=EQUAL path=bestFriend.hobby constant=hiking");

    assertExpressional(person.bestFriend
                    .value().with(f -> f.hobby.is("hiking").hobby.is("ski")),
            "call op=AND",
            "  call op=EQUAL path=bestFriend.hobby constant=hiking",
            "  call op=EQUAL path=bestFriend.hobby constant=ski");

    assertExpressional(person.bestFriend
                    .value().with(f -> f.hobby.is("hiking").or().hobby.is("ski")),
            "call op=OR",
            "  call op=EQUAL path=bestFriend.hobby constant=hiking",
            "  call op=EQUAL path=bestFriend.hobby constant=ski");

    assertExpressional(person.bestFriend
                    .value().with(f -> f.hobby.is("hiking").or().hobby.is("ski"))
                    .fullName.isEmpty(),
            "call op=AND",
            "  call op=OR",
            "    call op=EQUAL path=bestFriend.hobby constant=hiking",
            "    call op=EQUAL path=bestFriend.hobby constant=ski",
            "  call op=EQUAL path=fullName constant="
    );

    assertExpressional(person.bestFriend.value().hobby.isEmpty()
                    .with(p -> p.bestFriend.value().hobby.is("hiking").or()
                            .bestFriend.value().hobby.is("ski"))
                    .fullName.is("John"),
            "call op=AND",
            "  call op=EQUAL path=bestFriend.hobby constant=",
            "  call op=OR",
            "    call op=EQUAL path=bestFriend.hobby constant=hiking",
            "    call op=EQUAL path=bestFriend.hobby constant=ski",
            "  call op=EQUAL path=fullName constant=John"
    );
  }

  private static void assertExpressional(Criterion<?> crit, String ... expectedLines) {
    final StringBuilder out = new StringBuilder();
    Query query = Criterias.toQuery(crit);
    query.filter().ifPresent(f -> f.accept(new DebugExpressionVisitor<>(out)));
    final String expected = Arrays.stream(expectedLines).collect(Collectors.joining(System.lineSeparator()));
    Assertions.assertEquals(expected, out.toString().trim());
  }

}
