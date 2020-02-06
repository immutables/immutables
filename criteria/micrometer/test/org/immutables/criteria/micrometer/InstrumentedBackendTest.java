package org.immutables.criteria.micrometer;

import static org.hamcrest.Matchers.*;
import static org.immutables.check.Checkers.check;
import static org.immutables.criteria.personmodel.PersonCriteria.person;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Scope;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.ContainerNaming;
import org.immutables.criteria.backend.WithSessionCallback;
import org.immutables.criteria.backend.WriteResult;
import org.immutables.criteria.geode.GeodeBackend;
import org.immutables.criteria.geode.GeodeExtension;
import org.immutables.criteria.geode.GeodeSetup;
import org.immutables.criteria.personmodel.ImmutablePerson;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.immutables.criteria.personmodel.PersonRepository;
import org.immutables.criteria.repository.Publishers;
import org.immutables.criteria.repository.reactive.ReactiveReadable;
import org.immutables.criteria.repository.reactive.ReactiveWatchable;
import org.immutables.criteria.repository.reactive.ReactiveWritable;
import org.immutables.value.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.reactivestreams.Publisher;

import com.google.common.collect.Iterables;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(GeodeExtension.class)
class InstrumentedBackendTest {

    private final Backend backend;

    private PersonRepository personRepository;
    private ReactiveModelRepository reactiveModelRepository;

    private SimpleMeterRegistry meterRegistry;

    private PersonRepository instrumentedPersonRepository;
    private ReactiveModelRepository instrumentedReactiveModelRepository;

    public InstrumentedBackendTest(Cache cache) {
        this.backend = WithSessionCallback.wrap(new GeodeBackend(GeodeSetup.of(cache)), new CreateRegionCallback(cache));
    }

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        personRepository = new PersonRepository(backend);
        reactiveModelRepository = new ReactiveModelRepository(backend);

        final Backend instrumentedBackend = InstrumentedBackend.instrument(backend).with(meterRegistry);

        instrumentedPersonRepository = new PersonRepository(instrumentedBackend);
        instrumentedReactiveModelRepository = new ReactiveModelRepository(instrumentedBackend);
    }

    @Test
    void createsTimerAndMeasuresElapsedDuration() {
        final PersonGenerator personGenerator = new PersonGenerator();
        personRepository.insert(ImmutablePerson.copyOf(personGenerator.next()).withAge(20).withFullName("Joe Bloggs"));

        check(meterRegistry.getMeters()).isEmpty();

        check(instrumentedPersonRepository.find(
                person.age.greaterThan(18)
                        .fullName.is("Joe Bloggs")
        ).count()).is(1L);

        final Meter meter = getOnlyMeter();

        final Meter.Id meterId = meter.getId();
        check(meterId.getName()).is("criteria.geodebackend.operations.select");
        check(meterId.getTags()).hasSize(1);

        final Tag tag = Iterables.getOnlyElement(meterId.getTags());

        check(tag.getKey()).is("query");
        check(tag.getValue()).is("SELECT COUNT(*) FROM person WHERE (age > $1) AND (fullName = $2)");

        check(meter).is(instanceOf(Timer.class));
        final Timer timer = meterRegistry.timer(meterId.getName(), meterId.getTags());

        check(timer.count()).is(1L);
        check(timer.totalTime(TimeUnit.MILLISECONDS)).is(greaterThan(0d));
    }

    @Test
    void createsTimerAndOnPublisherSubscribeMeasuresElapsedDuration() throws InterruptedException {
        Publishers.blockingGet(reactiveModelRepository.insert(ImmutableReactiveModel.builder().foo("bar").build()));
        check(Publishers.blockingGet(reactiveModelRepository.findAll().count())).is(1L);

        final Publisher<WriteResult> writeResultPublisher = instrumentedReactiveModelRepository.delete(ReactiveModelCriteria.reactiveModel.foo.is("bar"));

        final Timer timer = meterRegistry.timer("criteria.geodebackend.operations.delete");
        check(timer.count()).is(0L);
        check(timer.totalTime(TimeUnit.NANOSECONDS)).is(0d);

        Publishers.blockingGet(writeResultPublisher);

        check(Publishers.blockingGet(reactiveModelRepository.findAll().count())).is(0L);

        final Meter meter = getOnlyMeter();

        Assertions.assertSame(timer, meter);

        final double totalTimeAfterFirstDelete = timer.totalTime(TimeUnit.NANOSECONDS);
        check(timer.count()).is(1L);
        check(totalTimeAfterFirstDelete).is(greaterThanOrEqualTo(0d));

        Publishers.blockingGet(writeResultPublisher);

        final double totalTimeAfterSecondDelete = timer.totalTime(TimeUnit.NANOSECONDS);
        check(timer.count()).is(2L);
        check(totalTimeAfterSecondDelete).is(greaterThanOrEqualTo(totalTimeAfterFirstDelete));
    }

    private Meter getOnlyMeter() {
        final List<Meter> meters = meterRegistry.getMeters();

        check(meters).hasSize(1);

        return Iterables.getOnlyElement(meters);
    }

    @Value.Immutable
    @Criteria
    @Criteria.Repository(facets = {ReactiveReadable.class, ReactiveWritable.class, ReactiveWatchable.class})
    public interface ReactiveModel {

        @Criteria.Id
        String foo();

    }

    private static class CreateRegionCallback implements Consumer<Class<?>> {
        private final Cache cache;

        private CreateRegionCallback(Cache cache) {
            this.cache = cache;
        }

        @Override
        public void accept(Class<?> aClass) {
            final String name = ContainerNaming.DEFAULT.name(aClass);

            if (cache.getRegion(name) == null) {
                cache.createRegionFactory()
                        .setScope(Scope.LOCAL)
                        .setValueConstraint((Class<Object>) aClass)
                        .create(name);
            }

        }
    }
}