package org.immutables.criteria.geode;

import static org.immutables.check.Checkers.check;
import static org.immutables.criteria.personmodel.PersonCriteria.person;

import org.immutables.criteria.backend.ImmutableSelect;
import org.immutables.criteria.backend.ImmutableUpdate;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Iterables;

import io.micrometer.core.instrument.Tag;

class OqlTagGeneratorTest {

    private OqlTagGenerator oqlTagGenerator;

    @BeforeEach
    void setUp() {
        oqlTagGenerator = new OqlTagGenerator(Person.class);
    }

    @Test
    void generatesTagForSelectOperation() {
        final Query query = Query.of(Person.class)
                .withFilter(Matchers.toExpression(person.age.greaterThan(18)));
        final StandardOperations.Select select = ImmutableSelect.builder().query(query).build();

        final Iterable<Tag> tags = oqlTagGenerator.generate(select);

        check(tags).hasSize(1);

        final Tag tag = Iterables.getOnlyElement(tags);
        check(tag.getKey()).is("query");
        check(tag.getValue()).is("SELECT * FROM person WHERE age > $1");
    }

    @Test
    void generatesEmptyTagForOtherOperations() {
        final PersonGenerator personGenerator = new PersonGenerator();
        final StandardOperations.Update update = ImmutableUpdate.builder()
                .addValues(personGenerator.next())
                .build();

        final Iterable<Tag> tags = oqlTagGenerator.generate(update);

        check(tags).isEmpty();
    }

}