package org.immutables.fixture.jackson;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style
public interface JacksonMappedWithNoAnnotations {
  String getSomeString();
}
