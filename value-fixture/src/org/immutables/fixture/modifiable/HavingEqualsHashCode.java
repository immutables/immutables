package org.immutables.fixture.modifiable;

import org.immutables.value.Value;

@Value.Modifiable
@Value.Immutable(intern = true)
public abstract class HavingEqualsHashCode {
  @Override
  public final boolean equals(Object o) {
    return o.toString().equals(this.toString());
  }

  @Override
  public final int hashCode() {
    return 1;
  }
}
