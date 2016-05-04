package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

public interface DefaultTypingProblem {
  @Value.Immutable
  @JsonSerialize(as = ImmutableOuterObject.class)
  @JsonDeserialize(as = ImmutableOuterObject.class)
  public static abstract class OuterObject {
    public abstract EmptyObject emptyObject();
  }

  @Value.Immutable
  @JsonSerialize(as = ImmutableEmptyObject.class)
  @JsonDeserialize(as = ImmutableEmptyObject.class)
  public static abstract class EmptyObject {}
}
