package org.immutables.mongo.fixture.criteria;

import com.google.common.collect.Range;
import org.immutables.mongo.fixture.MongoContext;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Pattern;

import static org.immutables.check.Checkers.check;

public class PersonCriteriaTest {
  @Rule
  public final MongoContext context = MongoContext.create();

  private final PersonRepository repository = new PersonRepository(context.setup());

  /**
   * Criteria based on int
   */
  @Test
  public void age() throws Exception {
    Person john = ImmutablePerson.builder().id("p1").name("John").age(30).build();
    repository.insert(john).getUnchecked();

    check(repository.find(repository.criteria().age(30)).fetchAll().getUnchecked()).hasSize(1);
    check(repository.find(repository.criteria().age(31)).fetchAll().getUnchecked()).isEmpty();
//    check(repository.find(repository.criteria().ageNot(30)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageNot(31)).fetchAll().getUnchecked()).hasSize(1);
    check(repository.find(repository.criteria().ageAtLeast(29)).fetchAll().getUnchecked()).hasSize(1);
    check(repository.find(repository.criteria().ageAtLeast(30)).fetchAll().getUnchecked()).hasSize(1);
    check(repository.find(repository.criteria().ageAtLeast(31)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageAtMost(31)).fetchAll().getUnchecked()).hasSize(1);
    check(repository.find(repository.criteria().ageAtMost(30)).fetchAll().getUnchecked()).hasSize(1);
    check(repository.find(repository.criteria().ageAtMost(29)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageGreaterThan(29)).fetchAll().getUnchecked()).hasSize(1);
    check(repository.find(repository.criteria().ageGreaterThan(30)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageGreaterThan(31)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageIn(Arrays.asList(1, 2, 3))).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageIn(Arrays.asList(29, 30, 31))).fetchAll().getUnchecked())
            .hasSize(1);

    check(repository.find(repository.criteria().ageNotIn(30, 31)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageNotIn(1, 2)).fetchAll().getUnchecked()).hasSize(1);
    check(repository.find(repository.criteria().ageLessThan(1)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageLessThan(30)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageLessThan(31)).fetchAll().getUnchecked()).hasSize(1);

    // add second person
    Person adam = ImmutablePerson.builder().id("p2").name("Adam").age(40).build();
    repository.insert(adam).getUnchecked();

    check(repository.find(repository.criteria().age(30)).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().age(40)).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
    check(repository.find(repository.criteria().ageAtLeast(29)).fetchAll().getUnchecked()).hasContentInAnyOrder(john, adam);
    check(repository.find(repository.criteria().ageAtLeast(30)).fetchAll().getUnchecked()).hasContentInAnyOrder(john, adam);
    check(repository.find(repository.criteria().ageAtLeast(31)).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
    check(repository.find(repository.criteria().ageAtMost(31)).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().ageAtMost(30)).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().ageAtMost(29)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageGreaterThan(29)).fetchAll().getUnchecked()).hasContentInAnyOrder(john, adam);
    check(repository.find(repository.criteria().ageGreaterThan(30)).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
    check(repository.find(repository.criteria().ageGreaterThan(31)).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
    check(repository.find(repository.criteria().ageIn(Arrays.asList(1, 2, 3))).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageIn(Arrays.asList(29, 30, 40, 44))).fetchAll().getUnchecked()).hasContentInAnyOrder(john, adam);
    check(repository.find(repository.criteria().ageNotIn(30, 31)).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
    check(repository.find(repository.criteria().ageNotIn(1, 2)).fetchAll().getUnchecked()).hasContentInAnyOrder(john, adam);
    check(repository.find(repository.criteria().ageLessThan(1)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageLessThan(30)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageLessThan(31)).fetchAll().getUnchecked()).hasSize(1);
  }

  /**
   * This one seems to fail: ageNot(30) should return no result.
   */
  @Test
  @Ignore
  public void failingTest_to_be_checked() throws Exception {
    Person john = ImmutablePerson.builder().id("p1").name("John").age(30).build();
    repository.insert(john).getUnchecked();
    check(repository.find(repository.criteria().age(30)).fetchAll().getUnchecked()).hasSize(1);
    check(repository.find(repository.criteria().ageNot(30)).fetchAll().getUnchecked()).isEmpty();
  }

  @Test
  public void stringPattern() throws Exception {
    Person john = ImmutablePerson.builder().id("p1").name("John").age(30).build();
    repository.insert(john).getUnchecked();

    check(repository.find(repository.criteria().name("John")).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().name("John123")).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().nameNot("John")).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().nameStartsWith("John")).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().nameStartsWith("Jo")).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().nameStartsWith("J")).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().nameIn("J1", "J2")).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().nameIn("John", "John")).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().nameNotIn("John", "John")).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().nameNotIn("J1", "J2")).fetchAll().getUnchecked()).hasContentInAnyOrder(john);

    // patterns
    check(repository.find(repository.criteria().nameMatches(Pattern.compile("J.*n"))).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().nameMatches(Pattern.compile("J\\w+n"))).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().nameMatches(Pattern.compile("J..n"))).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().nameMatches(Pattern.compile(".*"))).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().nameNotMatches(Pattern.compile("J.*n"))).fetchAll().getUnchecked()).isEmpty();
  }

  @Test
  public void subCollection() throws Exception {
    Person john = ImmutablePerson.builder().id("p1").name("John").age(30).build();
    check(repository.find(repository.criteria().aliasesEmpty()).fetchAll().getUnchecked()).isEmpty();

    repository.insert(john).getUnchecked();
    check(repository.find(repository.criteria().aliasesEmpty()).fetchAll().getUnchecked()).hasContentInAnyOrder(john);

    Person adam = ImmutablePerson.builder().id("p2").name("Adam").age(40).aliases(Arrays.asList("a1", "a2")).build();

    repository.insert(adam).getUnchecked();
    check(repository.find(repository.criteria().aliasesEmpty()).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().aliasesNonEmpty()).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
    check(repository.find(repository.criteria().aliasesContains("a1")).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
    check(repository.find(repository.criteria().aliasesContains("a2")).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
//    check(repository.find(repository.criteria().aliasesContainsAll(Arrays.asList("a1", "a2"))).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
//    check(repository.find(repository.criteria().aliasesContainsAll(Arrays.asList("a2", "a1"))).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
    check(repository.find(repository.criteria().aliasesContainsAll(Collections.singletonList("a1"))).fetchAll().getUnchecked()).isEmpty();

    check(repository.find(repository.criteria().aliasesAnyStartsWith("a")).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
    check(repository.find(repository.criteria().aliasesAnyStartsWith("a1")).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
    check(repository.find(repository.criteria().aliasesAnyStartsWith("b")).fetchAll().getUnchecked()).isEmpty();

    check(repository.find(repository.criteria().aliasesSize(0)).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().aliasesSize(1)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().aliasesSize(2)).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
    check(repository.find(repository.criteria().aliasesSize(3)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().aliasesSize(4)).fetchAll().getUnchecked()).isEmpty();
  }

  @Test
  public void negation() throws Exception {
    Date dob = new Date();
    Person john = ImmutablePerson.builder().id("p1").name("John").age(30).aliases(Collections.singleton("a1"))
            .dateOfBirth(dob).build();
    repository.insert(john).getUnchecked();


    // id
    check(repository.find(repository.criteria().idNot(john.id())).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().idNot("__BAD__")).fetchAll().getUnchecked()).hasSize(1);
    check(repository.find(repository.criteria().idNotIn(john.id(), "aaa")).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().idNotIn(Collections.singleton(john.id()))).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().idNotIn(Collections.singleton("__BAD__"))).fetchAll().getUnchecked()).hasSize(1);

    // name
    check(repository.find(repository.criteria().nameNot(john.name())).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().nameNotMatches(Pattern.compile("J..n"))).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().nameNotMatches(Pattern.compile("A...B"))).fetchAll().getUnchecked()).hasSize(1);

    // age this seems to be a bug (?)
    // check(repository.find(repository.criteria().ageNot(john.age())).fetchAll().getUnchecked()).isEmpty();

    check(repository.find(repository.criteria().ageNotIn(john.age(), john.age())).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageNotIn(Collections.singleton(john.age()))).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().ageNotIn(Range.open(john.age() + 1, john.age() + 2))).fetchAll().getUnchecked()).hasSize(1);

    check(repository.find(repository.criteria().aliasesNonEmpty()).fetchAll().getUnchecked()).hasSize(1);

    check(repository.find(repository.criteria().dateOfBirthNot(dob)).fetchAll().getUnchecked()).isEmpty();

  }

  @Test
  public void or() throws Exception {
    Person john = ImmutablePerson.builder().id("p1").name("John").age(30).build();
    Person adam = ImmutablePerson.builder().id("a1").name("Adam").age(44).build();

    repository.insert(john).getUnchecked();
    repository.insert(adam).getUnchecked();

    check(repository.find(repository.criteria().age(30).or().age(44)).fetchAll().getUnchecked()).hasContentInAnyOrder(john, adam);
    check(repository.find(repository.criteria().age(1).or().age(2)).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().age(30).or().age(2)).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().age(1).or().age(44)).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
    check(repository.find(repository.criteria().age(30).or().name("Adam")).fetchAll().getUnchecked()).hasContentInAnyOrder(john, adam);
    check(repository.find(repository.criteria().name("Adam").or().age(30)).fetchAll().getUnchecked()).hasContentInAnyOrder(john, adam);
    check(repository.find(repository.criteria().name("Adam").or().name("Adam").or().name("Adam")).fetchAll().getUnchecked()).hasContentInAnyOrder(adam);
    check(repository.find(repository.criteria().name("a").or().name("b").or().name("c")).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().id("p1").or().name("John").or().age(30)).fetchAll().getUnchecked()).hasContentInAnyOrder(john);
    check(repository.find(repository.criteria().id("p1").or().idNot("p1")).fetchAll().getUnchecked()).hasContentInAnyOrder(john, adam);

  }

  @Test
  public void empty() throws Exception {
    check(repository.findAll().fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria()).fetchAll().getUnchecked()).isEmpty();
  }
}