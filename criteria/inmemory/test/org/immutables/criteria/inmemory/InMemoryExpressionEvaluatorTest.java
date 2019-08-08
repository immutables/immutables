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

package org.immutables.criteria.inmemory;

import org.immutables.criteria.Criterias;
import org.immutables.criteria.personmodel.Address;
import org.immutables.criteria.personmodel.ImmutableAddress;
import org.immutables.criteria.personmodel.ImmutablePerson;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.immutables.check.Checkers.check;

public class InMemoryExpressionEvaluatorTest {

  private final ImmutablePerson example = ImmutablePerson.builder()
          .id("abc123")
          .fullName("John")
          .age(22)
          .dateOfBirth(LocalDate.now())
          .isActive(false)
          .address(ImmutableAddress.builder().city("Washington").state(Address.State.NY)
                  .street("aaa").zip("12221").build())
          .nickName("007")
          .build();

  @Test
  public void reflection() {
    final PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;

    final ImmutablePerson person = example.withFullName("John");

    check(!evaluate(crit.age.is(11), person));
    check(evaluate(crit.age.isNot(11), person));
    check(evaluate(crit.age.is(11), person.withAge(11)));
    check(evaluate(crit.age.is(22).fullName.is("John"), person));
    check(!evaluate(crit.age.is(22).fullName.is("Marry"), person));

    check(!evaluate(crit.age.in(1, 2, 3), person));
    check(evaluate(crit.age.notIn(1, 2, 3), person));
    check(evaluate(crit.age.in(22, 23, 24), person));
    check(!evaluate(crit.age.notIn(22, 23, 24), person));
    check(!evaluate(crit.isActive.isTrue(), person));
    check(evaluate(crit.isActive.isFalse(), person));
    check(evaluate(crit.isActive.isTrue().or().isActive.isFalse(), person));
    check(evaluate(crit.isActive.isFalse().or()
            .isActive.isTrue()
            .nickName.isAbsent(), person));

    check(!evaluate(crit.age.atLeast(23), person));
    check(evaluate(crit.age.atMost(22), person));
    check(!evaluate(crit.age.lessThan(22), person));
    check(!evaluate(crit.age.greaterThan(22), person));
    check(!evaluate(crit.age.atLeast(23), person));

    // optionals
    check(evaluate(crit.nickName.isPresent(), person));
    check(!evaluate(crit.nickName.isAbsent(), person));
    check(!evaluate(crit.nickName.isPresent(), person.withNickName(Optional.empty())));
    check(evaluate(crit.nickName.isAbsent(), person.withNickName(Optional.empty())));

    // == value().$expr
    check(!evaluate(crit.nickName.value().isNot("Smith"), person.withNickName("Smith")));
    check(evaluate(crit.nickName.value().in("Smith", "Nobody"), person.withNickName("Smith")));
    check(!evaluate(crit.nickName.value().in("Nobody", "Sky"), person.withNickName("Smith")));
    check(evaluate(crit.nickName.value().notIn("Nobody", "Sky"), person.withNickName("Smith")));

    // == value($expr)
    check(evaluate(crit.nickName.value().with(v -> v.is("Smith")), person.withNickName("Smith")));
    check(!evaluate(crit.nickName.value().with(v -> v.isNot("Smith")), person.withNickName("Smith")));
    check(evaluate(crit.nickName.value().with(v -> v.in("Smith", "Nobody")), person.withNickName("Smith")));
    check(!evaluate(crit.nickName.value().with(v -> v.in("Nobody", "Sky")), person.withNickName("Smith")));
    check(evaluate(crit.nickName.value().with(v -> v.notIn("Nobody", "Sky")), person.withNickName("Smith")));
  }

  @Test
  public void booleans() {
    final PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.person;

    final ImmutablePerson person = example.withFullName("A");

    check(evaluate(crit.nickName.isAbsent().or().nickName.isPresent(), person));
    check(!evaluate(crit.nickName.isAbsent().nickName.isPresent(), person));
    check(!evaluate(crit.fullName.is("A").fullName.is("B"), person));
    check(evaluate(crit.fullName.is("B").or().fullName.is("A"), person));
    check(!evaluate(crit.fullName.is("B").or().fullName.is("C"), person));
    check(!evaluate(crit.fullName.is("A").fullName.is("C"), person));
  }

  @Test
  public void empty() {
    final ImmutablePerson person = example;
    check(evaluate(PersonCriteria.person, person));
    check(evaluate(PersonCriteria.person, person.withFullName("llll")));
  }

  private static boolean evaluate(PersonCriteria criteria, Person person) {
    return Criterias.toQuery(criteria).filter().map(f -> InMemoryExpressionEvaluator.of(f).test(person)).orElse(true);
  }

}
