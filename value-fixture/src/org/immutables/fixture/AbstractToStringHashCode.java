package org.immutables.fixture;

import org.immutables.value.Value;

public class AbstractToStringHashCode {

  @Value.Immutable
  static abstract class AbstractHashCode {
    @Override
    public abstract int hashCode();
  }

  @Value.Immutable
  static abstract class AbstractToString {
    @Override
    public abstract String toString();
  }

  @Value.Immutable(intern = true)
  static abstract class InternCustomHashCode {
    abstract int a();

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof InternCustomHashCode;
    }
  }
}
