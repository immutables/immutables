package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableLazyAttributesSafe.class)
public abstract class LazyAttributesSafe {
  public abstract int getA();

  @Value.Lazy
  public String getLazy() {
    return String.valueOf(getA());
  }

  @Value.Derived
  public String getDerived() {
    return String.valueOf(getA());
  }

  @Value.Default
  public int getDefaults() {
    return getA();
  }
}
