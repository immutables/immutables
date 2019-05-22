package org.immutables.criteria;

import org.immutables.criteria.constraints.DebugExpressionVisitor;
import org.junit.Ignore;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.immutables.check.Checkers.check;

public class PersonTest {

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

  @Test
  @Ignore("used for compile-time testing only")
  public void collection() {
    PersonCriteria.create()
            .friends.any().nickName.isNotEmpty()
            .aliases.none().contains("foo")
            .or()//.or() should not work
            .isMarried.isTrue()
            .or()
            .friends.all().nickName.isNotEmpty()
            .friends.any().nickName.isEmpty()
            .friends.none().nickName.hasSize(3)
            .friends.all(f -> f.nickName.isEmpty().or().nickName.hasSize(2))
            .friends.any(f -> f.nickName.isEmpty().or().nickName.hasSize(2))
            .friends.none(f -> f.nickName.hasSize(3).nickName.startsWith("a"));
  }

  @Test
  public void debug() {
    PersonCriteria<PersonCriteria.Self> crit = PersonCriteria.create()
            .lastName.isAbsent()
            .or()
            .bestFriend.nickName.isNotEmpty()
            .or()
            .age.isGreaterThan(22)
            .firstName.isEqualTo("John");

    StringWriter out = new StringWriter();
    crit.expression().accept(new DebugExpressionVisitor<>(new PrintWriter(out)));
    check(out.toString()).isNonEmpty();
  }

  private static boolean evaluate(PersonCriteria criteria, Person person) {
    return InMemoryExpressionEvaluator.of(criteria.expression()).test(person);
  }
}