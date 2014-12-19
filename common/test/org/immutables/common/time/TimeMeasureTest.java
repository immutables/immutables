/*
    Copyright 2013-2014 Immutables Authors and Contributors

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
package org.immutables.common.time;

import org.immutables.common.time.TimeMeasure;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class TimeMeasureTest {
  @Test(expected = IllegalArgumentException.class)
  public void negativeValueConstructor() throws Exception {
    TimeMeasure.of(-1, TimeUnit.DAYS);
  }

  @Test(expected = NullPointerException.class)
  public void nullUnitConstructor() throws Exception {
    TimeMeasure.of(1, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void tooPreciseUnitConstructor() throws Exception {
    TimeMeasure.of(2, TimeUnit.NANOSECONDS);
  }

  @Test
  public void createParseCompare() throws Exception {
    TimeMeasure d2 = TimeMeasure.of(2, TimeUnit.DAYS);

    check(TimeMeasure.fromString("2d")).is(d2);
    check(TimeMeasure.hours(4)).hasToString("4h");
    check(d2.compareTo(TimeMeasure.hours(49)) < 0);
    check(TimeMeasure.minutes(1).compareTo(TimeMeasure.fromString("60s"))).is(0);
    check(d2.hashCode()).not(TimeMeasure.hours(2).hashCode());
  }

  @Test
  public void adding() throws Exception {
    check(TimeMeasure.days(2).plus(TimeMeasure.hours(2))).is(TimeMeasure.hours(50));
    check(TimeMeasure.millis(1).plus(TimeMeasure.seconds(1))).is(TimeMeasure.millis(1001));
  }

  @Test
  public void equality() throws Exception {
    check(TimeMeasure.days(1)).is(TimeMeasure.hours(24));
    check(!TimeMeasure.days(1).sameMeasure(TimeMeasure.hours(24)));
    check(TimeMeasure.days(1).sameMeasure(TimeMeasure.days(1)));
    check(TimeMeasure.millis(1000).hashCode()).is(TimeMeasure.seconds(1).hashCode());
    check(TimeMeasure.seconds(2).hashCode()).not(TimeMeasure.seconds(1).hashCode());
  }

  @Test
  public void sleepInterruption() {
    Thread.currentThread().interrupt();
    check(!TimeMeasure.millis(1).sleep());
    check(Thread.interrupted());
    // clearing interrupted status
    try {
      Thread.sleep(1);
    } catch (InterruptedException ex) {
      check(!Thread.interrupted());
    }
  }
}
