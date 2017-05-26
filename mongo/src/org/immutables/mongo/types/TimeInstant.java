/*
   Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.mongo.types;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Longs;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.Immutable;

/**
 * An instant in time in UTC milliseconds, without any timezone binding.
 * Corresponds to BSON builtin UTCDate type.
 */
@Immutable
public final class TimeInstant implements Comparable<TimeInstant> {
  private final long value;

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

  public boolean isBefore(TimeInstant that) {
    return compareTo(that) < 0;
  }

  public boolean isAfter(TimeInstant that) {
    return compareTo(that) > 0;
  }

  @Override
  public int compareTo(TimeInstant that) {
    return Longs.compare(value, that.value);
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
    return MoreObjects.toStringHelper(this)
        .addValue(value)
        .toString();
  }
}
