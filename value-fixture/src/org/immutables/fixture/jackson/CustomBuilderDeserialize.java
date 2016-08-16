package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCustomBuilderDeserialize.Builder.class)
public abstract class CustomBuilderDeserialize {
  public abstract int a();

  public abstract int b();
}