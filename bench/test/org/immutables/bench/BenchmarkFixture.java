package org.immutables.bench;

import com.google.common.collect.UnmodifiableIterator;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import java.util.Iterator;
import java.util.concurrent.Callable;
import org.immutables.common.time.TimeMeasure;
import org.immutables.service.ServiceManagers;

public class BenchmarkFixture extends AbstractModule {

  public static void main(String... args) {
    Injector injector = Guice.createInjector(
        new BenchmarkFixture(),
        ImmutableBenchmarkConfiguration.builder()
            .executionRepeatCount(200)
            .executionRandomLimit(100)
            .build());

    ServiceManagers.forServiceSet(injector)
        .startAsync()
        .awaitStopped();
  }

  @Override
  protected void configure() {}

  @Provides
  Iterable<Callable<Integer>> callable() {
    return new Iterable<Callable<Integer>>() {
      @Override
      public Iterator<Callable<Integer>> iterator() {
        return new UnmodifiableIterator<Callable<Integer>>() {
          @Override
          public boolean hasNext() {
            return true;
          }

          @Override
          public Callable<Integer> next() {
            return new Callable<Integer>() {
              @Override
              public Integer call() throws Exception {
                TimeMeasure.millis(3).sleep();
                return 1;
              }
            };
          }
        };
      }
    };
  }
}
