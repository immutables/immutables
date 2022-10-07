package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable

@Value.Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableStagedEntity.class)
public interface StagedEntity {
  String getRequired();
  @Value.Default
  default String getOptional() {
    return "default";
  }
  @Value.Default
  default boolean getOptionalPrimitive() {
    return false;
  }
}
