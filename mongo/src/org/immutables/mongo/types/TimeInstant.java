package org.immutables.mongo.types;

import javax.annotation.concurrent.Immutable;
import com.google.common.base.MoreObjects;
import com.google.common.primitives.Longs;
import java.util.concurrent.TimeUnit;

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
