package org.immutables.fixture.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable(builder = false)
@JsonSerialize(as = ImmutableConstructorJacksonMapped.class)
@JsonDeserialize(as = ImmutableConstructorJacksonMapped.class)
public interface ConstructorJacksonMapped {
  @Value.Parameter
  @JsonProperty("X")
  int instance();

  @Value.Parameter
  String bal();
}
