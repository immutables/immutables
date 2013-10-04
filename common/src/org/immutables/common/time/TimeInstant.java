/*
    Copyright 2013 Immutables.org authors

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

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.primitives.Longs;
import java.util.concurrent.TimeUnit;

/**
 * An instant in time in UTC milliseconds. (No timezone binding implied)
 */
public final class TimeInstant implements Comparable<TimeInstant> {
  private long value;

  private TimeInstant(long instant) {
    this.value = instant;
  }

  public static TimeInstant of(long instant) {
    return new TimeInstant(instant);
  }

  public long value() {
    return value;
  }

  public TimeUnit unit() {
    return TimeUnit.MILLISECONDS;
  }

  /**
   * Time measure in milliseconds elapsed since.
   * @param that the that
   * @return the time measure
   * @throws IllegalArgumentException if {@code that} is after {@code this}
   */
  public TimeMeasure elapsedSince(TimeInstant that) {
    return TimeMeasure.millis(value - that.value);
  }

  public boolean isBefore(TimeInstant that) {
    return compareTo(that) < 0;
  }

  public boolean isAfter(TimeInstant that) {
    return compareTo(that) > 0;
  }

  @Override
  public int compareTo(TimeInstant that) {
    return ComparisonChain.start()
        .compare(value, that.value)
        .result();
  }

  @Override
  public boolean equals(Object that) {
    return that instanceof TimeInstant && ((TimeInstant) that).value == value;
  }

  @Override
  public int hashCode() {
    return Longs.hashCode(value);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .addValue(value)
        .toString();
  }
}
