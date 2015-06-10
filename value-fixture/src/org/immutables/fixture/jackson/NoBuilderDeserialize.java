package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable(
    singleton = true,
    builder = false)
@Value.Style(instance = "instance")
@JsonDeserialize(as = ImmutableNoBuilderDeserialize.class)
public abstract class NoBuilderDeserialize {
  public abstract List<String> prop();
}
