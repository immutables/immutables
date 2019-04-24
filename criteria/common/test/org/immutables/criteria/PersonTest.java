package org.immutables.criteria;

import org.immutables.criteria.constraints.DebugExpressionVisitor;
import org.immutables.criteria.constraints.StringCriteria;
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
  }

  @Test
  @Ignore("TODO correct handling of empty / nil expressions")
  public void empty() {
    final ImmutablePerson person = ImmutablePerson.builder().firstName("John").age(22)
            .bestFriend(ImmutableFriend.builder().nickName("aaa").build())
            .isMarried(false).build();

    check(evaluate(PersonCriteria.create(), person));
  }

  @Test
  @Ignore("used for compile-time testing only")
  public void collection() {
    PersonCriteria.create()
            .friends.any().nickName.isNotEmpty()
            .or()//.or() should not work
            .isMarried.isTrue()
            .or()
            .friends.any(f -> f.nickName.isEmpty().or().nickName.hasSize(2))
            .friends.none(f -> f.nickName.hasSize(3).nickName.startsWith("a"))
            .aliases.none().contains("foo")
            .or()
            .lastName.value().isNotEmpty()
            .lastName.value().hasSize(2)
            .lastName.value(f -> f.startsWith("foo").endsWith("bar"))
            .lastName.value(f -> f.startsWith("foo").or().endsWith("bar"))
            .lastName.value(f -> f.isNotEmpty().isGreaterThan("aaa"))
            .lastName.value(StringCriteria::isNotEmpty)
            .bestFriend.nickName.startsWith("foo");
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
    crit.expression().accept(new DebugExpressionVisitor<>(new PrintWriter(out)), null);
    check(out.toString()).isNonEmpty();
  }

  private static boolean evaluate(PersonCriteria criteria, Person person) {
    return InMemoryExpressionEvaluator.of(criteria.expression()).test(person);
  }
}