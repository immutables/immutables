package org.immutables.criteria.inmemory;

import org.immutables.criteria.ImmutableFriend;
import org.immutables.criteria.ImmutablePerson;
import org.immutables.criteria.Person;
import org.immutables.criteria.PersonCriteria;
import org.junit.Test;

import static org.immutables.check.Checkers.check;

public class InMemoryExpressionEvaluatorTest {


  @Test
  public void reflection() {
    // TODO this inner class is ugly
    final PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.create();

    final ImmutablePerson person = ImmutablePerson.builder().firstName("John").age(22)
            .bestFriend(ImmutableFriend.builder().nickName("aaa").build())
            .isMarried(false).build();

    check(!evaluate(crit.age.isEqualTo(11), person));
    check(evaluate(crit.age.isNotEqualTo(11), person));
    check(evaluate(crit.age.isEqualTo(11), person.withAge(11)));
    check(evaluate(crit.age.isEqualTo(22).firstName.isEqualTo("John"), person));
    check(!evaluate(crit.age.isEqualTo(22).firstName.isEqualTo("Marry"), person));

    check(!evaluate(crit.age.isIn(1, 2, 3), person));
    check(evaluate(crit.age.isNotIn(1, 2, 3), person));
    check(evaluate(crit.age.isIn(22, 23, 24), person));
    check(!evaluate(crit.age.isNotIn(22, 23, 24), person));
    check(!evaluate(crit.isMarried.isTrue(), person));
    check(evaluate(crit.isMarried.isFalse(), person));
    check(evaluate(crit.isMarried.isTrue().or().isMarried.isFalse(), person));
    check(evaluate(crit.isMarried.isFalse().or()
            .isMarried.isTrue()
            .lastName.isAbsent(), person));

    check(!evaluate(crit.age.isAtLeast(23), person));
    check(evaluate(crit.age.isAtMost(22), person));
    check(!evaluate(crit.age.isLessThan(22), person));
    check(!evaluate(crit.age.isGreaterThan(22), person));

    check(!evaluate(crit.age.isAtLeast(23), person));
    check(evaluate(crit.lastName.isAbsent(), person));
    check(!evaluate(crit.lastName.isPresent(), person));
    check(evaluate(crit.lastName.isPresent(), person.withLastName("Smith")));
    check(!evaluate(crit.lastName.isAbsent(), person.withLastName("Smith")));

    // == value().$expr
    check(evaluate(crit.lastName.value().isEqualTo("Smith"), person.withLastName("Smith")));
    check(!evaluate(crit.lastName.value().isNotEqualTo("Smith"), person.withLastName("Smith")));
    check(evaluate(crit.lastName.value().isIn("Smith", "Nobody"), person.withLastName("Smith")));
    check(!evaluate(crit.lastName.value().isIn("Nobody", "Sky"), person.withLastName("Smith")));
    check(evaluate(crit.lastName.value().isNotIn("Nobody", "Sky"), person.withLastName("Smith")));

    // == value($expr)
    check(evaluate(crit.lastName.value(v -> v.isEqualTo("Smith")), person.withLastName("Smith")));
    check(!evaluate(crit.lastName.value(v -> v.isNotEqualTo("Smith")), person.withLastName("Smith")));
    check(evaluate(crit.lastName.value(v -> v.isIn("Smith", "Nobody")), person.withLastName("Smith")));
    check(!evaluate(crit.lastName.value(v -> v.isIn("Nobody", "Sky")), person.withLastName("Smith")));
    check(evaluate(crit.lastName.value(v -> v.isNotIn("Nobody", "Sky")), person.withLastName("Smith")));
  }

  @Test
  public void empty() {
    final ImmutablePerson person = ImmutablePerson.builder().firstName("John").age(22)
            .bestFriend(ImmutableFriend.builder().nickName("aaa").build())
            .isMarried(false).build();

    check(evaluate(PersonCriteria.create(), person));
    check(evaluate(PersonCriteria.create(), person.withLastName("llll")));
  }

  private static boolean evaluate(PersonCriteria criteria, Person person) {
    return InMemoryExpressionEvaluator.of(criteria.expression()).test(person);
  }

}
