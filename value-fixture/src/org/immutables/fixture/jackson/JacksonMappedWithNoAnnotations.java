package org.immutables.fixture.jackson;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.GenerateJacksonMappings;

@Value.Immutable
@Value.Style(generateJacksonMappings = GenerateJacksonMappings.ALWAYS)
public interface JacksonMappedWithNoAnnotations {
  String getSomeString();
}
