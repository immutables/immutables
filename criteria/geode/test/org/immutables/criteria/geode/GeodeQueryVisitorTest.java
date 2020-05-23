package org.immutables.criteria.geode;

import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.personmodel.ImmutablePet;
import org.immutables.criteria.typemodel.DateHolderCriteria;
import org.immutables.criteria.typemodel.EnumHolderCriteria;
import org.immutables.criteria.typemodel.TypeHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;

import static org.immutables.check.Checkers.check;
import static org.immutables.criteria.personmodel.PersonCriteria.person;

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

        check(toOqlWithBindParams(person.fullName.isEmpty())).is("fullName = $1");
        check(toOqlWithBindParams(person.fullName.notEmpty())).is("fullName != $1");
    }

    @Test
    void definedUndefined() {
        check(toOql(person.address.isPresent())).is("is_defined(address) AND address != null");
        check(toOql(person.address.isAbsent())).is("is_undefined(address) OR address = null");

        check(toOql(person.address.value().zip4.isPresent())).is("is_defined(address.zip4) AND address.zip4 != null");
        check(toOql(person.address.value().zip4.isAbsent())).is("is_undefined(address.zip4) OR address.zip4 = null");
    }

    @Test
    void filterNested() {
        check(toOql(person.address.value().city.is("London")))
                .is("address.city = 'London'");
    }

    /**
     * When not using bind variables enum shouold be converted to {@code enum.name == 'value'}
     */
    @Test
    void enumWithoutBindVariable() {
        EnumHolderCriteria holder = EnumHolderCriteria.enumHolder;
        check(toOql(holder.value.is(TypeHolder.Foo.ONE))).is("value.name = 'ONE'");
        check(toOql(holder.value.isNot(TypeHolder.Foo.ONE))).is("value.name != 'ONE'");
        check(toOql(holder.value.in(Arrays.asList(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO)))).is("value.name IN SET('ONE', 'TWO')");
        check(toOql(holder.value.notIn(Arrays.asList(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO)))).is("NOT (value.name IN SET('ONE', 'TWO'))");

        // nullable
        check(toOql(holder.nullable.is(TypeHolder.Foo.ONE))).is("nullable.name = 'ONE'");
        check(toOql(holder.nullable.isNot(TypeHolder.Foo.ONE))).is("nullable.name != 'ONE'");
        check(toOql(holder.nullable.in(Arrays.asList(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO)))).is("nullable.name IN SET('ONE', 'TWO')");
        check(toOql(holder.nullable.notIn(Arrays.asList(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO)))).is("NOT (nullable.name IN SET('ONE', 'TWO'))");
    }

    @Test
    void booleanWithoutBindVariable() {
        check(toOql(person.isActive.is(true))).is("isActive = true");
        check(toOql(person.isActive.is(false))).is("isActive = false");
        check(toOql(person.isActive.isNot(true))).is("isActive != true");
        check(toOql(person.isActive.isNot(false))).is("isActive != false");
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
    void dates() throws ParseException {
        String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        DateHolderCriteria date = DateHolderCriteria.dateHolder;
        Date value = format.parse("2020-01-22 23:00:00.000");
        check(toOql(date.value.is(value))).is("value = to_date('2020-01-22 23:00:00.000', 'yyyy-MM-dd HH:mm:ss.SSS')");
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

    @Test
    void upperLower() {
        check(toOql(person.fullName.toUpperCase().is("A"))).is("fullName.toUpperCase = 'A'");
        check(toOql(person.fullName.toLowerCase().is("A"))).is("fullName.toLowerCase = 'A'");
        check(toOql(person.fullName.toLowerCase().isNot("A"))).is("fullName.toLowerCase != 'A'");
        check(toOql(person.fullName.toLowerCase().in(Arrays.asList("a", "b")))).is("fullName.toLowerCase IN SET('a', 'b')");
        check(toOql(person.fullName.toLowerCase().notIn(Arrays.asList("a", "b")))).is("NOT (fullName.toLowerCase IN SET('a', 'b'))");
        check(toOql(person.fullName.toLowerCase().toUpperCase().is("A"))).is("fullName.toLowerCase.toUpperCase = 'A'");
        check(toOql(person.fullName.toLowerCase().endsWith("A"))).is("fullName.toLowerCase.endsWith('A')");
    }

    private static String toOqlWithBindParams(Criterion<?> criteria) {
        return toOql(criteria, true);
    }

    private static String toOql(Criterion<?> criteria) {
        return toOql(criteria, false);
    }

    private static String toOql(Criterion<?> criteria, boolean useBindVariables) {
        final PathNaming pathNaming = ReservedWordNaming.of(PathNaming.defaultNaming());
        final GeodeQueryVisitor queryVisitor = new GeodeQueryVisitor(useBindVariables, pathNaming);
        Expression filter = Criterias.toQuery(criteria).filter().orElseThrow(() -> new IllegalStateException("no filter"));
        return filter.accept(queryVisitor).oql();
    }

}