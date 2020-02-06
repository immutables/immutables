package org.immutables.criteria.geode;

import static org.immutables.check.Checkers.check;
import static org.immutables.criteria.matcher.Matchers.toExpression;
import static org.immutables.criteria.personmodel.PersonCriteria.person;

import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class GeodeQueryVisitorTest {

    @Test
    void filter() {
        check(toOql(person.age.is(18))).is("age = $1");
        check(toOql(person.age.isNot(18))).is("age != $1");

        check(toOql(person.age.atMost(18))).is("age <= $1");
        check(toOql(person.age.lessThan(18))).is("age < $1");

        check(toOql(person.age.atLeast(18))).is("age >= $1");
        check(toOql(person.age.greaterThan(18))).is("age > $1");

        check(toOql(person.age.between(18, 21))).is("(age >= $1) AND (age <= $2)");

        check(toOql(person.address.isPresent())).is("address != null");
        check(toOql(person.address.isAbsent())).is("address = null");

        check(toOql(person.fullName.isEmpty())).is("fullName = $1");
        check(toOql(person.fullName.notEmpty())).is("fullName != $1");
    }

    @Test
    @Disabled("Disabled until collections supported")
    void filter_collection() {
        check(toOql(person.interests.isEmpty())).is("interests.isEmpty");
        check(toOql(person.interests.notEmpty())).is("NOT interests.isEmpty");
        check(toOql(person.interests.hasSize(1))).is("interests.size = $1");
        check(toOql(person.interests.contains("OSS"))).is("interests.contains($1)");
    }

    @Test
    void filter_nested() {
        check(toOql(person.address.value().city.is("London")))
                .is("address.city = $1");
    }

    @Test
    void filter_negation() {
        check(toOql(person.age.not(self -> self.greaterThan(18)))).is("NOT (age > $1)");
    }

    @Test
    void filter_in() {
        check(toOql(person.age.in(18, 19, 20, 21))).is("age IN $1");

        check(toOql(person.age.notIn(18, 19, 20, 21))).is("NOT (age IN $1)");
    }

    @Test
    void filter_conjunction() {
        check(toOql(person.age.greaterThan(18)
                .address.value().city.is("London")
                .and(person.isActive.isTrue())))
                .is("((age > $1) AND (address.city = $2)) AND (isActive = $3)");
    }

    @Test
    void filter_disjunction() {
        check(toOql(person.age.greaterThan(18)
                .or(person.isActive.isTrue()))).is("(age > $1) OR (isActive = $2)");
    }

    @Test
    void filter_conjunction_with_disjunction() {
        check(toOql(person.age.greaterThan(18)
                .address.value().city.is("London")
                .or(person.isActive.isTrue()))).is("((age > $1) AND (address.city = $2)) OR (isActive = $3)");
    }

    private static String toOql(PersonCriteria personCriteria) {
        final PathNaming pathNaming = ReservedWordNaming.of(PathNaming.defaultNaming());
        final GeodeQueryVisitor queryVisitor = new GeodeQueryVisitor(true, pathNaming);
        return toExpression(personCriteria).accept(queryVisitor).oql();
    }
}