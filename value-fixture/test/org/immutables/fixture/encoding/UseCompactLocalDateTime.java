package org.immutables.fixture.encoding;

import java.util.Date;
import org.immutables.fixture.encoding.defs.CompactDateEnabled;
import org.immutables.value.Value;

@CompactDateEnabled
public abstract class UseCompactLocalDateTime {
  public abstract static class Base {
    @Value.Default
    public Date firstSeen() {
      return new Date();
    }

    @Value.Default
    public Date lastSeen() {
      return firstSeen();
    }
  }

  @Value.Immutable
  public abstract static class SomeSubclass extends Base {
    public abstract String name();
  }
}
