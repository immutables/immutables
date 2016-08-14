package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

// Compilation test for how Json objects include abstract toString and hashCode
// even if they looks like accessors and was not previously correctly handled
@JsonSerialize(as = ImmutableToStringAndHashCodeAbstract.class)
@JsonDeserialize(as = ImmutableToStringAndHashCodeAbstract.class)
@Value.Immutable
public abstract class ToStringAndHashCodeAbstract extends Exception {
  public abstract long test();

  @Override
  public abstract String toString();

  @Override
  public abstract int hashCode();
}
