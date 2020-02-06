package org.immutables.criteria.micrometer;

import java.util.concurrent.TimeUnit;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.StandardOperations;
import org.immutables.criteria.geode.OqlTagGenerator;
import org.reactivestreams.Publisher;

import com.google.common.base.Stopwatch;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.reactivex.Flowable;

public class InstrumentedBackend implements Backend {

    private final Backend delegate;
    private final MeterRegistry meterRegistry;

    public static InstrumentedBackend.Builder instrument(Backend backend) {
        return new Builder(backend);
    }

    InstrumentedBackend(MeterRegistry meterRegistry, Backend delegate) {
        this.delegate = delegate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Session open(Class<?> entityType) {
        final Session sessionDelegate = delegate.open(entityType);
        return new InstrumentedSession(meterRegistry, delegate, sessionDelegate);
    }

    public static class Builder {

        private final Backend backend;

        public Builder(Backend backend) {
            this.backend = backend;
        }

        public InstrumentedBackend with(MeterRegistry registry) {
            return new InstrumentedBackend(registry, backend);
        }
    }

    private static class InstrumentedSession implements Session {

        private final MeterRegistry meterRegistry;
        private final Session delegate;
        private final MeterNamingStrategy meterNamingStrategy;
        private final TagGenerator tagGenerator;

        public InstrumentedSession(MeterRegistry meterRegistry, Backend backend, Session delegate) {
            this.delegate = delegate;
            this.meterRegistry = meterRegistry;
            this.meterNamingStrategy = new MeterNamingStrategy(backend);
            this.tagGenerator = new OqlTagGenerator(delegate.entityType());
        }

        @Override
        public Class<?> entityType() {
            return delegate.entityType();
        }

        @Override
        public Result execute(Operation operation) {
            if (operation instanceof StandardOperations.Watch) {
                return delegate.execute(operation);
            } else {
                final String meterName = meterNamingStrategy.getMeterName(operation);
                final Iterable<Tag> tags = tagGenerator.generate(operation);
                final Timer timer = meterRegistry.timer(meterName, tags);
                return new InstrumentedResult(timer, delegate, operation);
            }
        }
    }

    private static class InstrumentedResult implements Result {

        private final Timer timer;
        private final Session sessionDelegate;
        private final Operation operation;

        public InstrumentedResult(Timer timer,
                                  Session sessionDelegate,
                                  Operation operation) {
            this.timer = timer;
            this.sessionDelegate = sessionDelegate;
            this.operation = operation;
        }

        @Override
        public <T> Publisher<T> publisher() {
            return Flowable.fromCallable(Stopwatch::createStarted)
                    .flatMap(stopwatch -> {
                        final Result result = sessionDelegate.execute(operation);
                        return Flowable.fromPublisher(result.<T>publisher())
                                .doOnTerminate(() -> {
                                    final TimeUnit desiredUnit = TimeUnit.MILLISECONDS;
                                    final long elapsed = stopwatch.stop().elapsed(desiredUnit);
                                    timer.record(elapsed, desiredUnit);
                                });
                    });
        }
    }

}
