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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractScheduledService.Scheduler;
import java.util.concurrent.TimeUnit;
import org.immutables.annotation.GenerateCheck;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.common.time.TimeMeasure;
import static com.google.common.base.Preconditions.*;

@GenerateImmutable
@GenerateMarshaler
public abstract class ScheduleConfiguration {

  public abstract Optional<TimeMeasure> initialDelay();

  public abstract Optional<TimeMeasure> rate();

  public abstract Optional<TimeMeasure> delay();

  @GenerateCheck
  protected void check() {
    checkState(rate().isPresent() ^ delay().isPresent(), "Only one of `rate` or `delay` should be set");
  }

  public Scheduler newScheduler() {
    long initialDelay =
        initialDelay().isPresent()
            ? initialDelay().get().toMillis()
            : 0;

    if (delay().isPresent()) {
      return Scheduler.newFixedDelaySchedule(
          initialDelay,
          delay().get().toMillis(),
          TimeUnit.MILLISECONDS);
    }

    return Scheduler.newFixedRateSchedule(
        initialDelay,
        rate().get().toMillis(),
        TimeUnit.MILLISECONDS);
  }
}
