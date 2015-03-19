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
}
