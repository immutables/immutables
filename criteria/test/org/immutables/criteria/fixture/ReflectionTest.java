package org.immutables.criteria.fixture;

import org.immutables.criteria.constraints.ReflectionEvaluator;
import org.junit.Test;

import static org.immutables.check.Checkers.check;

public class ReflectionTest {

  @Test
  public void reflection() {
    final PersonCriteria crit = PersonCriteria.create();
    final ImmutablePerson person = ImmutablePerson.builder().firstName("John").age(22).isMarried(false).build();

    check(evaluate(crit, person));
    check(!evaluate(crit.age.isEqualTo(11), person));
    check(evaluate(crit.age.isEqualTo(11), person.withAge(11)));
    check(evaluate(crit.age.isEqualTo(22).firstName.isEqualTo("John"), person));
    check(!evaluate(crit.age.isEqualTo(22).firstName.isEqualTo("Marry"), person));
    check(evaluate(crit.age.isEqualTo(22).firstName.isEqualTo("Marry").or()
            .firstName.isEqualTo("John"), person));

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

  @Test
  public void disjunction() {
    final PersonCriteria crit = PersonCriteria.create();
    final ImmutablePerson person = ImmutablePerson.builder().firstName("John").age(22).isMarried(false).build();

    check(evaluate(crit.age.isEqualTo(21).or().age.isEqualTo(22), person));
    check(!evaluate(crit.age.isEqualTo(1).or().age.isEqualTo(2), person));
    check(evaluate(crit.age.isEqualTo(1).or().firstName.isEqualTo("John"), person));
    check(!evaluate(crit.age.isEqualTo(1).or().firstName.isNotEqualTo("John"), person));
  }

  @Test
  public void weird() {
    // hmm weird DSL
    PersonCriteria.create().or().or().age.isLessThan(2);
    PersonCriteria.create().age.isLessThan(2).or().or();
  }

  private static boolean evaluate(PersonCriteria criteria, Person person) {
    return ReflectionEvaluator.of(criteria).apply(person);
  }
}