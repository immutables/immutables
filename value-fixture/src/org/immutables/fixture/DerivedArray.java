package org.immutables.fixture;

import java.nio.charset.StandardCharsets;
import org.immutables.value.Value;

@Value.Immutable
public abstract class DerivedArray {
  public abstract String foo();

  public abstract byte[] goo();

  @Value.Derived
  public byte[] bar() {
    return foo().getBytes(StandardCharsets.UTF_8);
  }
}
