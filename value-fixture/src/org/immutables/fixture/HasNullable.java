package org.immutables.fixture;

import org.immutables.value.Json;
import org.immutables.value.Value;
import javax.annotation.Nullable;

@Value.Immutable(singleton = true)
@Json.Marshaled
public abstract class HasNullable {
  @Nullable
  @Value.Parameter
  @Json.ForceEmpty
  public abstract Integer in();

  @Nullable
  @Value.Default
  public String def() {
    return null;
  }

  @Nullable
  @Value.Derived
  public String der() {
    return null;
  }
}
