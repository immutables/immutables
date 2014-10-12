package org.immutables.generate.silly;

import org.immutables.value.Value;

@Value.Immutable
public abstract class SillyExtendedBuilder {

  public static class Builder {
    public final boolean base = true;
  }
}
