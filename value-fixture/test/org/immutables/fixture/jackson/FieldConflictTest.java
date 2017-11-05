/*
   Copyright 2016 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.fixture.jackson;

import static org.immutables.check.Checkers.check;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;
import org.junit.Test;

@Value.Style(forceJacksonIgnoreFields = true)
public final class FieldConflictTest {
  @JsonSerialize
  @JsonDeserialize
  @Value.Immutable
  public abstract static class Dummy {
    @Value.Parameter
    public abstract boolean isSomeProperty();
  }

  @JsonSerialize
  @JsonDeserialize
  @Value.Immutable
  public abstract static class CustomDummy {
    @Value.Parameter
    @JsonProperty("custom_name")
    public abstract boolean isSomeProperty();
  }

  @JsonSerialize
  @JsonDeserialize
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.PACKAGE, ElementType.TYPE})
  public @interface Model {}

  @Model
  @Value.Immutable
  public abstract static class DummyWithMetaAnnotation {
    @Value.Parameter
    public abstract boolean isSomeProperty();
  }

  @Model
  @Value.Immutable
  public abstract static class CustomDummyWithMetaAnnotation {
    @Value.Parameter
    @JsonProperty("custom_name")
    public abstract boolean isSomeProperty();
  }

  @Test
  public void dummyWithDefaultObjectMapper() throws IOException {
    verifyRoundTrip(getMapper(false), ImmutableDummy.of(true));
  }

  @Test
  public void dummyWithCustomObjectMapper() throws IOException {
    verifyRoundTrip(getMapper(true), ImmutableDummy.of(true));
  }

  @Test
  public void customDummyWithDefaultObjectMapper() throws IOException {
    verifyRoundTrip(getMapper(false), ImmutableCustomDummy.of(true));
  }

  // Fails: Unrecognized field "isSomeProperty" (class
// com.picnic.fulfillment.model.ImmutableCustomDummy$Json), not marked as ignorable (one known
// property: "custom_name"])
  @Test
  public void customDummyWithCustomObjectMapper() throws IOException {
    verifyRoundTrip(getMapper(true), ImmutableCustomDummy.of(true));
  }

  @Test
  public void dummyWithMetaAnnotationWithDefaultObjectMapper() throws IOException {
    verifyRoundTrip(getMapper(false), ImmutableDummyWithMetaAnnotation.of(true));
  }

  public void dummyWithMetaAnnotationWithCustomObjectMapper() throws IOException {
    verifyRoundTrip(getMapper(true), ImmutableDummyWithMetaAnnotation.of(true));
  }

  @Test
  public void customDummyWithMetaAnnotationWithDefaultObjectMapper() throws IOException {
    verifyRoundTrip(getMapper(false), ImmutableCustomDummyWithMetaAnnotation.of(true));
  }

  @Test
  public void customDummyWithMetaAnnotationWithCustomObjectMapper() throws IOException {
    verifyRoundTrip(getMapper(true), ImmutableCustomDummyWithMetaAnnotation.of(true));
  }

  private static ObjectMapper getMapper(final boolean useFields) {
    final ObjectMapper mapper = new ObjectMapper();
    return useFields
        ? mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        : mapper;
  }

  private static void verifyRoundTrip(final ObjectMapper mapper, final Object value) throws IOException {
    final String json = mapper.writeValueAsString(value);
    final Object newValue = mapper.readValue(json, value.getClass());
    check(newValue).is(value);
  }
}
