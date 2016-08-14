package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

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
