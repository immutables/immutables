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

import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.primitives.Longs;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnegative;
import static com.google.common.base.Preconditions.*;

public final class TimeMeasure implements Comparable<TimeMeasure> {
  private static final Pattern TIME_MEASURE_PATTERN = Pattern.compile("^(\\d+)([a-zA-Z]{1,2})?$");

  private final long value;
  private final TimeUnit unit;

  private TimeMeasure(@Nonnegative long value, TimeUnit unit) {
    checkNotNull(unit, "TimeUnit must be provided");
    checkArgument(unit.ordinal() >= 2, "Unsupported precision %s", unit);
    checkArgument(value >= 0, "Unsupported negative values");

    this.unit = unit;
    this.value = value;
  }

  public TimeUnit unit() {
    return unit;
  }

  public long value() {
    return value;
  }

  public static TimeMeasure fromString(String text) {
    Matcher matcher = TIME_MEASURE_PATTERN.matcher(text);
    if (matcher.matches()) {
      long value = Long.parseLong(matcher.group(1));
      String unitString = Strings.nullToEmpty(matcher.group(2)).toLowerCase();
      try {
        return new TimeMeasure(value, unitFromString(unitString));
      } catch (Exception ex) {
        // intended to fallback to exception in the end of method
      }
    }

    throw new IllegalArgumentException("TimeMeasure parse error for input string: " + text);
  }

  private static TimeUnit unitFromString(String unitString) {
    switch (unitString) {
    case "":
    case "ms":
      return TimeUnit.MILLISECONDS;
    case "s":
      return TimeUnit.SECONDS;
    case "m":
      return TimeUnit.MINUTES;
    case "h":
      return TimeUnit.HOURS;
    case "d":
      return TimeUnit.DAYS;
    }
    throw new IllegalArgumentException();
  }

  public boolean sleep() {
    try {
      if (value > 0) {
        unit.sleep(value);
      }
      return true;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Longs.hashCode(toMillis());
  }

  public boolean sameMeasure(TimeMeasure that) {
    return (value == 0 && that.value == 0)
        || (value == that.value && unit == that.unit);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof TimeMeasure) {
      TimeMeasure that = (TimeMeasure) obj;
      return toMillis() == that.toMillis();
    }

    return false;
  }

  public TimeMeasure as(TimeUnit timeUnit) {
    return new TimeMeasure(timeUnit.convert(value, unit), timeUnit);
  }

  public long toMillis() {
    return unit.toMillis(value);
  }

  public long toSeconds() {
    return unit.toSeconds(value);
  }

  public long toMinutes() {
    return unit.toMinutes(value);
  }

  public long toHours() {
    return unit.toHours(value);
  }

  public long toDays() {
    return unit.toDays(value);
  }

  @Override
  public String toString() {
    switch (unit) {
    case SECONDS:
      return value + "s";
    case MINUTES:
      return value + "m";
    case HOURS:
      return value + "h";
    case DAYS:
      return value + "d";
    default:
      return value + "ms";
    }
  }

  public TimeMeasure add(TimeMeasure o) {
    return unit.compareTo(o.unit) < 0
        ? new TimeMeasure(value + unit.convert(o.value, o.unit), unit)
        : new TimeMeasure(o.value + o.unit.convert(value, unit), o.unit);
  }

  public int compareTo(TimeMeasure that) {
    return ComparisonChain.start()
        .compare(toMillis(), that.toMillis())
        .result();
  }

  public static TimeMeasure of(@Nonnegative long value, TimeUnit unit) {
    return new TimeMeasure(value, unit);
  }

  public static TimeMeasure millis(@Nonnegative long millis) {
    return new TimeMeasure(millis, TimeUnit.MILLISECONDS);
  }

  public static TimeMeasure seconds(@Nonnegative int seconds) {
    return new TimeMeasure(seconds, TimeUnit.SECONDS);
  }

  public static TimeMeasure minutes(@Nonnegative int minutes) {
    return new TimeMeasure(minutes, TimeUnit.MINUTES);
  }

  public static TimeMeasure hours(@Nonnegative int hours) {
    return new TimeMeasure(hours, TimeUnit.HOURS);
  }

  public static TimeMeasure days(@Nonnegative int days) {
    return new TimeMeasure(days, TimeUnit.DAYS);
  }
}
