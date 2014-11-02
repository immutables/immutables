package org.immutables.fixture;

import org.immutables.value.Value;

public class UnderwriteObjectMethods {
  @Value.Immutable
  public abstract static class Underwrite {
    public abstract int value();

    @Override
    public int hashCode() {
      return value() + 1;
    }

    @Override
    public boolean equals(Object obj) {
      return "U".equals(obj.toString());
    }

    @Override
    public String toString() {
      return "U";
    }
  }

  @Value.Immutable
  public abstract static class NoUnderwrite extends Underwrite {
    @Override
    public abstract int value();
  }
}
