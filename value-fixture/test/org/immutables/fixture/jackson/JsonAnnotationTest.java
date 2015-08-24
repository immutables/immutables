package org.immutables.fixture.jackson;

import static org.immutables.check.Checkers.check;

import java.lang.reflect.Field;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import nonimmutables.AdditionalJacksonAnnotation;

public class JsonAnnotationTest {
  @Test
  public void itPassesJsonAnnotations() throws NoSuchFieldException {
    Field declared = ImmutableJacksonMappedWithExtraAnnotation.Json.class.getDeclaredField("someLong");
    check(declared.getAnnotation(AdditionalJacksonAnnotation.class).value()).is("some_long");
    check(declared.getAnnotation(JsonProperty.class).value()).is("some_long_string");
    check(declared.getAnnotation(JsonSerialize.class).using()).is(CoreMatchers.equalTo(ToStringSerializer.class));
  }
}
