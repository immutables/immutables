package org.immutables.criteria.geode;

import static org.immutables.check.Checkers.check;
import static org.immutables.criteria.matcher.Matchers.toExpression;
import static org.immutables.criteria.personmodel.PersonCriteria.person;

import java.util.regex.Pattern;

import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.personmodel.ImmutablePet;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class GeodeQueryVisitorTest {

    @Test
    void filter() {
        check(toOql(person.age.is(18))).is("age = 18");
        check(toOql(person.age.isNot(18))).is("age != 18");

        check(toOql(person.age.atMost(18))).is("age <= 18");
        check(toOql(person.age.lessThan(18))).is("age < 18");

        check(toOql(person.age.atLeast(18))).is("age >= 18");
        check(toOql(person.age.greaterThan(18))).is("age > 18");

        check(toOql(person.age.between(18, 21))).is("(age >= 18) AND (age <= 21)");

        check(toOql(person.address.isPresent())).is("address != null");
        check(toOql(person.address.isAbsent())).is("address = null");
    }

    @Test
    void filterWithBindParams() {
        check(toOqlWithBindParams(person.age.is(18))).is("age = $1");
        check(toOqlWithBindParams(person.age.isNot(18))).is("age != $1");

        check(toOqlWithBindParams(person.age.atMost(18))).is("age <= $1");
        check(toOqlWithBindParams(person.age.lessThan(18))).is("age < $1");

        check(toOqlWithBindParams(person.age.atLeast(18))).is("age >= $1");
        check(toOqlWithBindParams(person.age.greaterThan(18))).is("age > $1");

        check(toOqlWithBindParams(person.age.between(18, 21))).is("(age >= $1) AND (age <= $2)");

        check(toOqlWithBindParams(person.address.isPresent())).is("address != null");
        check(toOqlWithBindParams(person.address.isAbsent())).is("address = null");

        check(toOqlWithBindParams(person.fullName.isEmpty())).is("fullName = $1");
        check(toOqlWithBindParams(person.fullName.notEmpty())).is("fullName != $1");
    }

    @Test
    void filterNested() {
        check(toOql(person.address.value().city.is("London")))
                .is("address.city = 'London'");
    }

    @Test
    void filterNestedWithBindParams() {
        check(toOqlWithBindParams(person.address.value().city.is("London")))
                .is("address.city = $1");
    }

    @Test
    void filterIn() {
        check(toOql(person.age.in(18, 19, 20, 21))).is("age IN SET(18, 19, 20, 21)");
        check(toOql(person.age.notIn(18, 19, 20, 21))).is("NOT (age IN SET(18, 19, 20, 21))");
    }

    @Test
    void filterInWithBindParams() {
        check(toOqlWithBindParams(person.age.in(18, 19, 20, 21))).is("age IN $1");
        check(toOqlWithBindParams(person.age.notIn(18, 19, 20, 21))).is("NOT (age IN $1)");
    }

    @Test
    void filterCollection() {
        check(toOql(person.interests.isEmpty())).is("interests.isEmpty");
        check(toOql(person.interests.notEmpty())).is("NOT (interests.isEmpty)");
        check(toOql(person.interests.hasSize(1))).is("interests.size = 1");
        check(toOql(person.interests.contains("OSS"))).is("interests.contains('OSS')");
    }

    @Test
    void filterCollectionWithBindParams() {
        check(toOqlWithBindParams(person.interests.isEmpty())).is("interests.isEmpty");
        check(toOqlWithBindParams(person.interests.notEmpty())).is("NOT (interests.isEmpty)");
        check(toOqlWithBindParams(person.interests.hasSize(1))).is("interests.size = $1");
        check(toOqlWithBindParams(person.interests.contains("OSS"))).is("interests.contains($1)");
    }

    @Disabled
    @Test
    void filterCollectionDoesNotSupportComplexTypes() {
        final ImmutablePet pet = ImmutablePet.builder().name("Rex").type(ImmutablePet.PetType.dog).build();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            toOql(person.pets.contains(pet));
        });
    }

    @Test
    void filterString() {
        check(toOql(person.fullName.isEmpty())).is("fullName = ''");
        check(toOql(person.fullName.notEmpty())).is("fullName != ''");

        check(toOql(person.fullName.hasLength(5))).is("fullName.length = 5");
        check(toOql(person.fullName.contains("Blog"))).isIn("fullName LIKE '%Blog%'", "fullName.contains('Blog')");
        check(toOql(person.fullName.startsWith("Joe"))).isIn("fullName LIKE 'Joe%'", "fullName.startsWith('Joe')");
        check(toOql(person.fullName.endsWith("Bloggs"))).isIn("fullName LIKE '%Bloggs'", "fullName.endsWith('Bloggs')");

        check(toOql(person.fullName.matches(Pattern.compile("\\w+")))).is("fullName.matches('\\w+')");
    }

    @Test
    void filterStringWithBindParams() {
        check(toOqlWithBindParams(person.fullName.isEmpty())).is("fullName = $1");
        check(toOqlWithBindParams(person.fullName.notEmpty())).is("fullName != $1");

        check(toOqlWithBindParams(person.fullName.hasLength(5))).is("fullName.length = $1");
        check(toOqlWithBindParams(person.fullName.contains("Blog"))).isIn("fullName LIKE $1", "fullName.contains($1)");
        check(toOqlWithBindParams(person.fullName.startsWith("Joe"))).isIn("fullName LIKE $1", "fullName.startsWith($1)");
        check(toOqlWithBindParams(person.fullName.endsWith("Bloggs"))).isIn("fullName LIKE $1", "fullName.endsWith($1)");

        check(toOqlWithBindParams(person.fullName.matches(Pattern.compile("\\w+")))).is("fullName.matches($1)");
    }

    @Test
    void filterNegation() {
        check(toOqlWithBindParams(person.age.not(self -> self.greaterThan(18)))).is("NOT (age > $1)");
    }

    @Test
    void filterConjunction() {
        check(toOqlWithBindParams(person.age.greaterThan(18)
                .address.value().city.is("London")
                .and(person.isActive.isTrue())))
                .is("(age > $1) AND (address.city = $2) AND (isActive = $3)");
    }

    @Test
    void filterDisjunction() {
        check(toOqlWithBindParams(person.age.greaterThan(18)
                .or(person.isActive.isTrue()))).is("(age > $1) OR (isActive = $2)");
    }

    @Test
    void filterConjunctionWithDisjunction() {
        check(toOqlWithBindParams(person.age.greaterThan(18)
                .address.value().city.is("London")
                .or(person.isActive.isTrue()))).is("((age > $1) AND (address.city = $2)) OR (isActive = $3)");
    }

    private static String toOqlWithBindParams(PersonCriteria personCriteria) {
        return toOql(personCriteria, true);
    }

    private static String toOql(PersonCriteria personCriteria) {
        return toOql(personCriteria, false);
    }

    private static String toOql(PersonCriteria personCriteria, boolean useBindVariables) {
        final PathNaming pathNaming = ReservedWordNaming.of(PathNaming.defaultNaming());
        final GeodeQueryVisitor queryVisitor = new GeodeQueryVisitor(useBindVariables, pathNaming);
        return toExpression(personCriteria).accept(queryVisitor).oql();
    }

}