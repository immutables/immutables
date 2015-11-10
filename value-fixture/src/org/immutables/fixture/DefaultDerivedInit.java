package org.immutables.fixture;

import java.util.UUID;
import org.immutables.value.Value;

@Value.Immutable
public abstract class DefaultDerivedInit {
  @Value.Derived
  public String index() {
    return uuid() + "!!!";
  }

  @Value.Parameter
  @Value.Default
  public String uuid() {
    return UUID.randomUUID().toString();
  }

  @Value.Parameter
  public abstract String title();
}
