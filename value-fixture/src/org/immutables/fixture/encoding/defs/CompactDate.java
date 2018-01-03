package org.immutables.fixture.encoding.defs;

import java.util.Date;
import org.immutables.encode.Encoding;

@Encoding
public class CompactDate {
  @Encoding.Impl
  private long time;

  @Encoding.Expose
  Date get() {
    return toDate(time);
  }

  @Encoding.Of
  static long fromDate(Date date) {
    return date == null ? 0L : date.getTime();
  }

  private static Date toDate(long time) {
    return time == 0L ? null : new Date(time);
  }
}
