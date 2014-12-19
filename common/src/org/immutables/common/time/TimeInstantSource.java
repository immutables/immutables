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

import com.google.common.base.Ticker;
import javax.annotation.Nonnegative;
import javax.annotation.concurrent.ThreadSafe;
import static com.google.common.base.Preconditions.*;

/**
 * The time instant source with millisecond precision. See convenient static factory methods of
 * {@link TimeInstantSource}.
 * <p>
 * <em>
 * Hint: Guava's {@link Ticker} has different purpose and precision.</em>
 */
@ThreadSafe
public abstract class TimeInstantSource {

  /**
   * Read milliseconds value that is considered current by {@link TimeInstantSource}.
   * @return the current millis
   */
  public abstract long read();

  public final TimeInstant now() {
    return TimeInstant.of(read());
  }

  /**
   * Convenient system source as wrapper for {@link System#currentTimeMillis()}
   * @return the time instant source
   */
  public static TimeInstantSource systemSource() {
    return SYSTEM_SOURCE;
  }

  private static final TimeInstantSource SYSTEM_SOURCE = new TimeInstantSource() {
    @Override
    public long read() {
      return System.currentTimeMillis();
    }

    @Override
    public String toString() {
      return TimeInstantSource.class.getSimpleName() + ".systemSource()";
    }
  };

  /**
   * Returns timesource that wraps original timesource, while descreeting returned values to some
   * modulo, like 5 minute or hour etc.
   * @param source the source time instant source
   * @param modulo the modulo value
   * @return the discreeting time instant source
   */
  public static TimeInstantSource discreetingFrom(final TimeInstantSource source, @Nonnegative final long modulo) {
    checkNotNull(source);
    checkArgument(modulo > 0);
    return new TimeInstantSource() {
      @Override
      public long read() {
        long instant = source.read();
        return instant - instant % modulo;
      }

      @Override
      public String toString() {
        return TimeInstantSource.class.getSimpleName() + ".discreetingFrom(" + source + ", " + modulo + ")";
      }
    };
  }

  /**
   * Returns timesource that has fixed offset from values read by original source. Offset could
   * be positive or negative and just added to value read by original source.
   * @param source the source time instant source
   * @param offsetMillis milliseconds to offset value, could be negative
   * @return the offseting time instant source
   */
  public static TimeInstantSource offsetingFrom(final TimeInstantSource source, final long offsetMillis) {
    checkNotNull(source);
    return new TimeInstantSource() {
      @Override
      public long read() {
        return source.read() + offsetMillis;
      }

      @Override
      public String toString() {
        return TimeInstantSource.class.getSimpleName() + ".offsetingFrom(" + source + ", " + offsetMillis + ")";
      }
    };
  }

  /**
   * The settable time instant source. Most suitable for testing or other unusual conditions
   */
  public static class SettableTimeInstantSource extends TimeInstantSource {
    private long instant;

    private SettableTimeInstantSource(long instant) {
      this.instant = instant;
    }

    /**
     * Sets the instant to specified value.
     * @param instant the instant
     */
    public synchronized void set(long instant) {
      this.instant = instant;
    }

    /**
     * Adjusts instant by delta, (could be any value, including negative).
     * @param delta the delta
     */
    public synchronized void adjust(long delta) {
      this.instant += delta;
    }

    /**
     * Simply returns stored instant.
     * @return stored intant value
     */
    @Override
    public synchronized long read() {
      return instant;
    }

    @Override
    public synchronized String toString() {
      return SettableTimeInstantSource.class.getSimpleName() + "(" + instant + ")";
    }
  }

  /**
   * New settable time instant source. Initialized by current system millis and can be set or
   * adjusted by calling code. This is mostly useful in testing.
   * @return the settable time instant source
   */
  public static SettableTimeInstantSource newSettableSource() {
    return new SettableTimeInstantSource(0);
  }
}
