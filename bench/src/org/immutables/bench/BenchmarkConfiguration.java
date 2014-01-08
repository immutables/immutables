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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
          .delay(TimeMeasure.millis(100))
          .build();

  @GenerateDefault
  public int queueCapacity() {
    return 1000;
  }

  @GenerateDefault
  public ScheduleConfiguration generateSchedule() {
    return DEFAULT_SCHEDULE;
  }

  @GenerateDefault
  public int workers() {
    return 10;
  }

  @GenerateDefault
  public int requestCount() {
    return 1;
  }

  @GenerateDefault
  public int requestRandomLimit() {
    return 0;
  }

  @GenerateDefault
  public ScheduleConfiguration sendSchedule() {
    return DEFAULT_SCHEDULE;
  }

  @Provides
  ScheduledExecutorService scheduledService() {
    return Executors.newScheduledThreadPool(10);
  }

  @Provides
  BenchmarkScheduleService requestGenerator(ScheduledExecutorService executor) {
    return new BenchmarkScheduleService(executor, generateSchedule(), new Runnable() {
      @Override
      public void run() {

      }
    });
  }

  @Override
  protected void configure() {

  }
}
