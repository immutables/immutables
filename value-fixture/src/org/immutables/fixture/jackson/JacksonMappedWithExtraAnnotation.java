package org.immutables.fixture.jackson;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import nonimmutables.AdditionalJacksonAnnotation;

@Value.Immutable
@Value.Style(additionalJsonAnnotations = { AdditionalJacksonAnnotation.class})
@JsonDeserialize(as = ImmutableJacksonMappedWithExtraAnnotation.class)
public interface JacksonMappedWithExtraAnnotation {

  @AdditionalJacksonAnnotation("not_name")
  String getName();
}
