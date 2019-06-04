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
    final PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.create();

    final ImmutablePerson person = example.withFullName("John");

    check(!evaluate(crit.age.isEqualTo(11), person));
    check(evaluate(crit.age.isNotEqualTo(11), person));
    check(evaluate(crit.age.isEqualTo(11), person.withAge(11)));
    check(evaluate(crit.age.isEqualTo(22).fullName.isEqualTo("John"), person));
    check(!evaluate(crit.age.isEqualTo(22).fullName.isEqualTo("Marry"), person));

    check(!evaluate(crit.age.isIn(1, 2, 3), person));
    check(evaluate(crit.age.isNotIn(1, 2, 3), person));
    check(evaluate(crit.age.isIn(22, 23, 24), person));
    check(!evaluate(crit.age.isNotIn(22, 23, 24), person));
    check(!evaluate(crit.isActive.isTrue(), person));
    check(evaluate(crit.isActive.isFalse(), person));
    check(evaluate(crit.isActive.isTrue().or().isActive.isFalse(), person));
    check(evaluate(crit.isActive.isFalse().or()
            .isActive.isTrue()
            .nickName.isAbsent(), person));

    check(!evaluate(crit.age.isAtLeast(23), person));
    check(evaluate(crit.age.isAtMost(22), person));
    check(!evaluate(crit.age.isLessThan(22), person));
    check(!evaluate(crit.age.isGreaterThan(22), person));
    check(!evaluate(crit.age.isAtLeast(23), person));

    // optionals
    check(evaluate(crit.nickName.isPresent(), person));
    check(!evaluate(crit.nickName.isAbsent(), person));
    check(!evaluate(crit.nickName.isPresent(), person.withNickName(Optional.empty())));
    check(evaluate(crit.nickName.isAbsent(), person.withNickName(Optional.empty())));

    // == value().$expr
    check(!evaluate(crit.nickName.value().isNotEqualTo("Smith"), person.withNickName("Smith")));
    check(evaluate(crit.nickName.value().isIn("Smith", "Nobody"), person.withNickName("Smith")));
    check(!evaluate(crit.nickName.value().isIn("Nobody", "Sky"), person.withNickName("Smith")));
    check(evaluate(crit.nickName.value().isNotIn("Nobody", "Sky"), person.withNickName("Smith")));

    // == value($expr)
    check(evaluate(crit.nickName.value(v -> v.isEqualTo("Smith")), person.withNickName("Smith")));
    check(!evaluate(crit.nickName.value(v -> v.isNotEqualTo("Smith")), person.withNickName("Smith")));
    check(evaluate(crit.nickName.value(v -> v.isIn("Smith", "Nobody")), person.withNickName("Smith")));
    check(!evaluate(crit.nickName.value(v -> v.isIn("Nobody", "Sky")), person.withNickName("Smith")));
    check(evaluate(crit.nickName.value(v -> v.isNotIn("Nobody", "Sky")), person.withNickName("Smith")));
  }

  @Test
  public void booleans() {
    final PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.create();

    final ImmutablePerson person = example.withFullName("A");

    check(evaluate(crit.nickName.isAbsent().or().nickName.isPresent(), person));
    check(!evaluate(crit.nickName.isAbsent().nickName.isPresent(), person));
    check(!evaluate(crit.fullName.isEqualTo("A").fullName.isEqualTo("B"), person));
    check(evaluate(crit.fullName.isEqualTo("B").or().fullName.isEqualTo("A"), person));
    check(!evaluate(crit.fullName.isEqualTo("B").or().fullName.isEqualTo("C"), person));
    check(!evaluate(crit.fullName.isEqualTo("A").fullName.isEqualTo("C"), person));
  }

  @Test
  public void empty() {
    final ImmutablePerson person = example;
    check(evaluate(PersonCriteria.create(), person));
    check(evaluate(PersonCriteria.create(), person.withFullName("llll")));
  }

  private static boolean evaluate(PersonCriteria criteria, Person person) {
    return InMemoryExpressionEvaluator.of(Criterias.toExpressional(criteria).expression()).test(person);
  }

}
