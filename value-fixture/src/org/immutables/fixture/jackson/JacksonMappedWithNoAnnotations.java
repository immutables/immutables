package org.immutables.fixture.jackson;

import org.immutables.value.Value;

@Value.Immutable
@MetaJacksonAnnotation
public interface JacksonMappedWithNoAnnotations {
  String getSomeString();
}
