package org.immutables.criteria.micrometer;

import static org.immutables.check.Checkers.check;
import static org.immutables.criteria.personmodel.PersonCriteria.person;

import java.util.Collections;

import org.immutables.criteria.backend.*;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.immutables.criteria.repository.FakeBackend;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MeterNamingStrategyTest {

    private PersonGenerator personGenerator;

    private MeterNamingStrategy meterNamingStrategy;

    @BeforeEach
    void setUp() {
        personGenerator = new PersonGenerator();
        meterNamingStrategy = new MeterNamingStrategy(new FakeBackend());
    }

    @Test
    void meterNameForSelectOperation() {
        final Query query = Query.of(Person.class)
                .withFilter(Matchers.toExpression(person.address.value().city.is("London")));

        final String meterName = meterNamingStrategy.getMeterName(ImmutableSelect.of(query));
        check(meterName).is("criteria.fakebackend.operations.select");
    }

    @Test
    void meterNameForInsertOperation() {
        final String meterName = meterNamingStrategy.getMeterName(
                ImmutableInsert.of(Collections.singleton(personGenerator.next()))
        );
        check(meterName).is("criteria.fakebackend.operations.insert");
    }

    @Test
    void meterNameForUpdateOperation() {
        final String meterName = meterNamingStrategy.getMeterName(
                ImmutableUpdate.builder().values(Collections.singletonList(personGenerator.next())).build()
        );
        check(meterName).is("criteria.fakebackend.operations.update");
    }

    @Test
    void meterNameForUpdateByQueryOperation() {
        final Query query = Query.of(Person.class)
                .withFilter(Matchers.toExpression(person.address.value().city.is("London")));

        final String meterName = meterNamingStrategy.getMeterName(
                ImmutableUpdateByQuery.builder()
                        .query(query)
                        .putValues(Matchers.toExpression(person.address.value().state), "Greater London")
                        .build()
        );
        check(meterName).is("criteria.fakebackend.operations.updatebyquery");
    }

    @Test
    void meterNameForDeleteOperation() {
        final String meterName = meterNamingStrategy.getMeterName(ImmutableDelete.of(Query.of(Person.class)));
        check(meterName).is("criteria.fakebackend.operations.delete");
    }

    @Test
    void meterNameForWatchOperationThrows() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            meterNamingStrategy.getMeterName(ImmutableWatch.of(Query.of(Person.class)));
        });
    }
}