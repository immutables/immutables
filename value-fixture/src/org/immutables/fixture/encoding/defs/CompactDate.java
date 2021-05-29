package org.immutables.fixture.encoding.defs;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import java.util.Date;
import java.util.Objects;
import org.immutables.encode.Encoding;

@Encoding
public class CompactDate {
  /**
   * Some
   * Java
   * Doc
   */
  @Encoding.Impl
  private long time;

  /** Have random annotations. */
  @Encoding.Expose
  @JsonAnyGetter
  @SuppressWarnings("deprecation")
  Date get() {
    return toDate(time);
  }

  @Encoding.Copy
  public long with(Date x) {
    Objects.requireNonNull(x);
    return x.getTime() + time;
  }

  @Encoding.Copy
  public long withL(long x) {
    long uu = x + 42;
    return uu;
  }

  @Encoding.Copy
  public long withI(int x) {
    return time + x;
  }

  @Encoding.Of
  static long fromDate(Date date) {
    return date == null ? 0L : date.getTime();
  }

  /** Copy It */
  @Deprecated
  private static Date toDate(long time) {
    return time == 0L ? null : new Date(time);
  }
}
