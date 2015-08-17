package org.immutables.fixture.jackson;

import static org.immutables.check.Checkers.check;

import org.junit.Test;

import nonimmutables.AdditionalJacksonAnnotation;

public class JsonAnnotationTest {
  @Test
  public void itPassesJsonAnnotations() throws NoSuchFieldException {
    check(ImmutableJacksonMappedWithExtraAnnotation.Json.class.getDeclaredField("name").getAnnotation(AdditionalJacksonAnnotation.class)).notNull();
  }
}
