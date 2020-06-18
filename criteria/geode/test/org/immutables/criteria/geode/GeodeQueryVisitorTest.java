package org.immutables.criteria.geode;

import org.immutables.check.StringChecker;
import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.typemodel.DateHolderCriteria;
import org.immutables.criteria.typemodel.EnumHolderCriteria;
import org.immutables.criteria.typemodel.TypeHolder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;

import static org.immutables.check.Checkers.check;
import static org.immutables.criteria.personmodel.PersonCriteria.person;

class GeodeQueryVisitorTest {

    /**
     *  Using bind variables in OQL ({@code $1, $2})
     */
    @Nested
    class BindVariables {

        @Test
        void filter() {
            oql(person.age.is(18)).is("age = $1");
            oql(person.age.isNot(18)).is("age != $1");

            oql(person.age.atMost(18)).is("age <= $1");
            oql(person.age.lessThan(18)).is("age < $1");

            oql(person.age.atLeast(18)).is("age >= $1");
            oql(person.age.greaterThan(18)).is("age > $1");

            oql(person.age.between(18, 21)).is("(age >= $1) AND (age <= $2)");

            oql(person.fullName.isEmpty()).is("fullName = $1");
            oql(person.fullName.notEmpty()).is("fullName != $1");
        }

        @Test
        void definedUndefined() {
            oql(person.address.isPresent()).is("is_defined(address) AND address != null");
            oql(person.address.isAbsent()).is("is_undefined(address) OR address = null");

            oql(person.address.value().zip4.isPresent()).is("is_defined(address.zip4) AND address.zip4 != null");
            oql(person.address.value().zip4.isAbsent()).is("is_undefined(address.zip4) OR address.zip4 = null");
        }

        @Test
        void filterNested() {
            oql(person.address.value().city.is("London"))
                    .is("address.city = $1");
        }

        @Test
        void filterIn() {
            oql(person.age.in(18, 19, 20, 21)).is("age IN $1");
            oql(person.age.notIn(18, 19, 20, 21)).is("NOT (age IN $1)");
        }

        @Test
        void filterCollection() {
            oql(person.interests.isEmpty()).is("interests.isEmpty()");
            oql(person.interests.notEmpty()).is("NOT (interests.isEmpty())");
            oql(person.interests.hasSize(1)).is("interests.size = $1");
            oql(person.interests.contains("OSS")).is("interests.contains($1)");
        }

        @Test
        void filterString() {
            oql(person.fullName.isEmpty()).is("fullName = $1");
            oql(person.fullName.notEmpty()).is("fullName != $1");

            oql(person.fullName.hasLength(5)).is("fullName.length = $1");
            oql(person.fullName.contains("Blog")).isIn("fullName LIKE $1", "fullName.contains($1)");
            oql(person.fullName.startsWith("Joe")).isIn("fullName LIKE $1", "fullName.startsWith($1)");
            oql(person.fullName.endsWith("Bloggs")).isIn("fullName LIKE $1", "fullName.endsWith($1)");
            oql(person.fullName.matches(Pattern.compile("\\w+"))).is("fullName.matches($1)");
        }

        @Test
        void filterNegation() {
            oql(person.age.not(self -> self.greaterThan(18))).is("NOT (age > $1)");
        }

        @Test
        void filterConjunction() {
            oql(person.age.greaterThan(18)
                    .address.value().city.is("London")
                    .and(person.isActive.isTrue()))
                    .is("(age > $1) AND (address.city = $2) AND (isActive = $3)");
        }

        @Test
        void filterDisjunction() {
            oql(person.age.greaterThan(18)
                    .or(person.isActive.isTrue())).is("(age > $1) OR (isActive = $2)");
        }

        @Test
        void filterConjunctionWithDisjunction() {
            oql(person.age.greaterThan(18)
                    .address.value().city.is("London")
                    .or(person.isActive.isTrue())).is("((age > $1) AND (address.city = $2)) OR (isActive = $3)");
        }

        @Test
        void upperLower() {
            oql(person.fullName.toUpperCase().is("A")).is("fullName.toUpperCase() = $1");
            oql(person.fullName.toLowerCase().is("A")).is("fullName.toLowerCase() = $1");
            oql(person.fullName.toLowerCase().isNot("A")).is("fullName.toLowerCase() != $1");
            oql(person.fullName.toLowerCase().in(Arrays.asList("a", "b"))).is("fullName.toLowerCase() IN $1");
            oql(person.fullName.toLowerCase().notIn(Arrays.asList("a", "b"))).is("NOT (fullName.toLowerCase() IN $1)");
            oql(person.fullName.toLowerCase().toUpperCase().is("A")).is("fullName.toLowerCase().toUpperCase() = $1");
            oql(person.fullName.toLowerCase().endsWith("A")).is("fullName.toLowerCase().endsWith($1)");
        }


        private StringChecker oql(Criterion<?> criterion) {
            return oqlChecker(criterion, true);
        }
    }

    /**
     * No bind variables. Raw OQL literals {@code true}, {@code 'foobar'}.
     */
    @Nested
    class WithoutBindVariables {
        @Test
        void filter() {
            oql(person.age.is(18)).is("age = 18");
            oql(person.age.isNot(18)).is("age != 18");

            oql(person.age.atMost(18)).is("age <= 18");
            oql(person.age.lessThan(18)).is("age < 18");

            oql(person.age.atLeast(18)).is("age >= 18");
            oql(person.age.greaterThan(18)).is("age > 18");
            oql(person.age.between(18, 21)).is("(age >= 18) AND (age <= 21)");
        }

        @Test
        void upperLower() {
            oql(person.fullName.toUpperCase().is("A")).is("fullName.toUpperCase() = 'A'");
            oql(person.fullName.toLowerCase().is("A")).is("fullName.toLowerCase() = 'A'");
            oql(person.fullName.toLowerCase().isNot("A")).is("fullName.toLowerCase() != 'A'");
            oql(person.fullName.toLowerCase().in(Arrays.asList("a", "b"))).is("fullName.toLowerCase() IN SET('a', 'b')");
            oql(person.fullName.toLowerCase().notIn(Arrays.asList("a", "b"))).is("NOT (fullName.toLowerCase() IN SET('a', 'b'))");
            oql(person.fullName.toLowerCase().toUpperCase().is("A")).is("fullName.toLowerCase().toUpperCase() = 'A'");
            oql(person.fullName.toLowerCase().endsWith("A")).is("fullName.toLowerCase().endsWith('A')");
        }

        @Test
        void filterNested() {
            oql(person.address.value().city.is("London"))
                    .is("address.city = 'London'");
        }

        /**
         * When not using bind variables enum shouold be converted to {@code enum.name == 'value'}
         */
        @Test
        void enums() {
            EnumHolderCriteria holder = EnumHolderCriteria.enumHolder;
            oql(holder.value.is(TypeHolder.Foo.ONE)).is("value.name = 'ONE'");
            oql(holder.value.isNot(TypeHolder.Foo.ONE)).is("value.name != 'ONE'");
            oql(holder.value.in(Arrays.asList(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO))).is("value.name IN SET('ONE', 'TWO')");
            oql(holder.value.notIn(Arrays.asList(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO))).is("NOT (value.name IN SET('ONE', 'TWO'))");

            // nullable
            oql(holder.nullable.is(TypeHolder.Foo.ONE)).is("nullable.name = 'ONE'");
            oql(holder.nullable.isNot(TypeHolder.Foo.ONE)).is("nullable.name != 'ONE'");
            oql(holder.nullable.in(Arrays.asList(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO))).is("nullable.name IN SET('ONE', 'TWO')");
            oql(holder.nullable.notIn(Arrays.asList(TypeHolder.Foo.ONE, TypeHolder.Foo.TWO))).is("NOT (nullable.name IN SET('ONE', 'TWO'))");
        }

        @Test
        void booleans() {
            oql(person.isActive.is(true)).is("isActive = true");
            oql(person.isActive.is(false)).is("isActive = false");
            oql(person.isActive.isNot(true)).is("isActive != true");
            oql(person.isActive.isNot(false)).is("isActive != false");
        }

        @Test
        void filterIn() {
            oql(person.age.in(18, 19, 20, 21)).is("age IN SET(18, 19, 20, 21)");
            oql(person.age.notIn(18, 19, 20, 21)).is("NOT (age IN SET(18, 19, 20, 21))");
        }

        @Test
        void filterCollection() {
            oql(person.interests.isEmpty()).is("interests.isEmpty()");
            oql(person.interests.notEmpty()).is("NOT (interests.isEmpty())");
            oql(person.interests.hasSize(1)).is("interests.size = 1");
            oql(person.interests.contains("OSS")).is("interests.contains('OSS')");
        }

        @Test
        void dates() throws ParseException {
            String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            DateHolderCriteria date = DateHolderCriteria.dateHolder;
            Date value = format.parse("2020-01-22 23:00:00.000");
            oql(date.value.is(value)).is("value = to_date('2020-01-22 23:00:00.000', 'yyyy-MM-dd HH:mm:ss.SSS')");
        }

        @Test
        void filterString() {
            oql(person.fullName.isEmpty()).is("fullName = ''");
            oql(person.fullName.notEmpty()).is("fullName != ''");

            oql(person.fullName.hasLength(5)).is("fullName.length = 5");
            oql(person.fullName.contains("Blog")).isIn("fullName LIKE '%Blog%'", "fullName.contains('Blog')");
            oql(person.fullName.startsWith("Joe")).isIn("fullName LIKE 'Joe%'", "fullName.startsWith('Joe')");
            oql(person.fullName.endsWith("Bloggs")).isIn("fullName LIKE '%Bloggs'", "fullName.endsWith('Bloggs')");
            oql(person.fullName.matches(Pattern.compile("\\w+"))).is("fullName.matches('\\w+')");
        }

        @Test
        void filterNegation() {
            oql(person.age.not(self -> self.greaterThan(18))).is("NOT (age > 18)");
        }

        @Test
        void filterConjunction() {
            oql(person.age.greaterThan(18)
                    .address.value().city.is("London")
                    .and(person.isActive.isTrue()))
                    .is("(age > 18) AND (address.city = 'London') AND (isActive = true)");
        }

        @Test
        void filterDisjunction() {
            oql(person.age.greaterThan(18)
                    .or(person.isActive.isTrue())).is("(age > 18) OR (isActive = true)");
        }

        @Test
        void filterConjunctionWithDisjunction() {
            oql(person.age.greaterThan(18)
                    .address.value().city.is("London")
                    .or(person.isActive.isTrue())).is("((age > 18) AND (address.city = 'London')) OR (isActive = true)");
        }


        private StringChecker oql(Criterion<?> criterion) {
            return oqlChecker(criterion, false);
        }
    }


    private static StringChecker oqlChecker(Criterion<?> criteria, boolean useBindVariables) {
        final PathNaming pathNaming = ReservedWordNaming.of(PathNaming.defaultNaming());
        final GeodeQueryVisitor queryVisitor = new GeodeQueryVisitor(useBindVariables, pathNaming);
        Expression filter = Criterias.toQuery(criteria).filter().orElseThrow(() -> new IllegalStateException("no filter"));
        return check(filter.accept(queryVisitor).oql());
    }

}