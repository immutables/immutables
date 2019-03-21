package org.immutables.criteria.fixture;

import org.junit.Ignore;
import org.junit.Test;

import static org.immutables.check.Checkers.check;

public class ReflectionTest {

  @Test
  @Ignore
  public void reflection() {
    final PersonCriteria crit = PersonCriteria.create();
    final ImmutablePerson person = ImmutablePerson.builder().firstName("John").age(22).isMarried(false).build();

    check(evaluate(crit, person));
    check(!evaluate(crit.age.isEqualTo(11), person));
    check(evaluate(crit.age.isEqualTo(11), person.withAge(11)));
    check(evaluate(crit.age.isEqualTo(22).firstName.isEqualTo("John"), person));
    check(!evaluate(crit.age.isEqualTo(22).firstName.isEqualTo("Marry"), person));

    check(!evaluate(crit.age.isIn(1, 2, 3), person));
    check(evaluate(crit.age.isIn(22, 23, 24), person));
    check(!evaluate(crit.isMarried.isTrue(), person));
    check(evaluate(crit.isMarried.isFalse(), person));
    check(evaluate(crit.isMarried.isEqualTo(false), person));

    check(evaluate(crit.age.isAtLeast(22), person));
    check(!evaluate(crit.age.isAtLeast(23), person));
    check(evaluate(crit.lastName.isEmpty(), person));
    check(!evaluate(crit.lastName.isPresent(), person));
    check(evaluate(crit.lastName.isPresent(), person.withLastName("Smith")));
    check(!evaluate(crit.lastName.isEmpty(), person.withLastName("Smith")));
  }


  private static boolean evaluate(PersonCriteria criteria, Person person) {
    return true;
  }
}