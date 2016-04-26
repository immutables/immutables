package org.immutables.fixture.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableProjectInformation.class)
@JsonDeserialize(as = ImmutableProjectInformation.class)
public interface ProjectInformation {

  @JsonProperty
  Integer getOrganizationId();
}
