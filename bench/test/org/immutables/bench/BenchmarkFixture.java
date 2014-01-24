package org.immutables.bench;

import com.google.common.collect.AbstractIterator;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import org.immutables.common.time.TimeMeasure;

public class BenchmarkFixture extends AbstractModule {

  private static final Key<Set<Service>> SERVICES = Key.get(new TypeLiteral<Set<Service>>() {});

  public static void main(String... args) {

    Injector injector = Guice.createInjector(
        new BenchmarkFixture(),
        ImmutableBenchmarkConfiguration.builder().build());

    new ServiceManager(injector.getInstance(SERVICES))
        .startAsync()
        .awaitStopped();
  }

  @Override
  protected void configure() {}

  @Provides
  Iterable<Callable<Integer>> callable() {
    return new RequestGenerator();
  }

  class RequestGenerator implements Iterable<Callable<Integer>> {
    @Override
    public Iterator<Callable<Integer>> iterator() {
      return new AbstractIterator<Callable<Integer>>() {
        @Override
        protected Callable<Integer> computeNext() {
          return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
              TimeMeasure.millis(100).sleep();
              return 1;
            }
          };
        }
      };
    }
  }
}
