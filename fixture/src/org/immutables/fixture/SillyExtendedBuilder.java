package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
abstract class SillyExtendedBuilder {

  public static class Builder {
    public final boolean inheritedField = true;
  }
}
