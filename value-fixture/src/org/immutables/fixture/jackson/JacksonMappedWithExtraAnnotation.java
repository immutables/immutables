package org.immutables.fixture.jackson;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import nonimmutables.AdditionalJacksonAnnotation;

@Value.Immutable
@Value.Style(additionalJsonAnnotations = { AdditionalJacksonAnnotation.class})
@JsonDeserialize(as = ImmutableJacksonMappedWithExtraAnnotation.class)
public interface JacksonMappedWithExtraAnnotation {

  @AdditionalJacksonAnnotation("some_long")
  @JsonProperty("some_long_string")
  @JsonSerialize(using=ToStringSerializer.class)
  Long getSomeLong();
}
