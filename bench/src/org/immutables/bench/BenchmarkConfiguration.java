/*
    Copyright 2013-2014 Immutables.org authors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.bench;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Named;
import javax.inject.Singleton;
import org.immutables.annotation.GenerateDefault;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.common.time.TimeMeasure;

@GenerateImmutable
@GenerateMarshaler
public abstract class BenchmarkConfiguration extends AbstractModule {

  private static final ImmutableScheduleConfiguration DEFAULT_SCHEDULE =
      ImmutableScheduleConfiguration.builder()
          .initialDelay(TimeMeasure.seconds(1))
          .delay(TimeMeasure.millis(50))
          .build();

  private static final ImmutableScheduleConfiguration DEFAULT_STAT_SCHEDULE =
      ImmutableScheduleConfiguration.builder()
          .initialDelay(TimeMeasure.seconds(1))
          .rate(TimeMeasure.seconds(1))
          .build();

  private static final ImmutableScheduleConfiguration DEFAULT_DISTRIBUTION_SCHEDULE =
      ImmutableScheduleConfiguration.builder()
          .initialDelay(TimeMeasure.seconds(5))
          .rate(TimeMeasure.seconds(5))
          .build();

  @GenerateDefault
  public int queueCapacity() {
    return 10000;
  }

  @GenerateDefault
  public ScheduleConfiguration enqueSchedule() {
    return DEFAULT_SCHEDULE;
  }

  @GenerateDefault
  public ScheduleConfiguration sampleStatsSchedule() {
    return DEFAULT_STAT_SCHEDULE;
  }

  @GenerateDefault
  public ScheduleConfiguration sampleDistributionSchedule() {
    return DEFAULT_DISTRIBUTION_SCHEDULE;
  }

  @GenerateDefault
  public String loggerName() {
    return BenchmarkConfiguration.class.getPackage().getName();
  }

  @GenerateDefault
  public ScheduleConfiguration executeSchedule() {
    return DEFAULT_SCHEDULE;
  }

  @GenerateDefault
  public int workers() {
    return 10;
  }

  @GenerateDefault
  public int executionRepeatCount() {
    return 1;
  }

  @GenerateDefault
  public int executionRandomLimit() {
    return 0;
  }

  @Singleton
  @Provides
  ScheduledExecutorService scheduledService() {
    return Executors.newScheduledThreadPool(10);
  }

  @Singleton
  @Provides
  Publisher publisher() {
    return new Publisher(loggerName());
  }

  @Provides
  @Singleton
  @Named("sampleStatsSchedule")
  BenchmarkScheduleService sampleStatsSchedule(ScheduledExecutorService executor, final Sampler sampler) {
    return new BenchmarkScheduleService(executor, sampleStatsSchedule(), new Runnable() {
      @Override
      public void run() {
        sampler.sampleStats();
      }
    });
  }

  @Provides
  @Singleton
  @Named("sampleDistributionSchedule")
  BenchmarkScheduleService sampleDistributionSchedule(ScheduledExecutorService executor, final Sampler sampler) {
    return new BenchmarkScheduleService(executor, sampleDistributionSchedule(), new Runnable() {
      @Override
      public void run() {
        sampler.sampleDistribution();
      }
    });
  }

  @Override
  protected void configure() {
    bind(Gatherer.class).in(Singleton.class);
    bind(Bombardier.class).in(Singleton.class);
    bind(BenchmarkConfiguration.class).toInstance(this);

    Multibinder<Service> serviceSet = Multibinder.newSetBinder(binder(), Service.class);

    serviceSet.addBinding().to(Key.get(BenchmarkScheduleService.class, Names.named("sampleStatsSchedule")));
    serviceSet.addBinding().to(Key.get(BenchmarkScheduleService.class, Names.named("sampleDistributionSchedule")));
    serviceSet.addBinding().to(Bombardier.class);
  }
}
