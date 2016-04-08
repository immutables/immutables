package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

/**
 * Compilation test for correct override of both equals and hashCode in Json integration class
 */
@JsonDeserialize(as = ImmutableHashCodeAbstract.class)
@Value.Immutable
public abstract class HashCodeAbstract extends InheritedWithHashCode {
  public abstract int someAttributeHaveToBeDefined();

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);
}

class InheritedWithHashCode {
  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    return false;
  }
}
