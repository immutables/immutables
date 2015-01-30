/*
    Copyright 2014 Immutables Authors and Contributors

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import java.io.IOException;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class ObjectMappedTest {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); {
    OBJECT_MAPPER.registerModule(new GuavaModule());
  }

  public static class Wrapper {
    public ImmutableSampleJacksonMapped mapped;
  }

  @Test
  public void constructorMapping() throws IOException {
    String originalSampleJson = "{\"X\":1, \"bal\": \"V\"}";
    ConstructorJacksonMapped mapped =
        OBJECT_MAPPER.readValue(originalSampleJson, ConstructorJacksonMapped.class);
    String json = OBJECT_MAPPER.writeValueAsString(mapped);
    check(OBJECT_MAPPER.readValue(json, ConstructorJacksonMapped.class)).is(mapped);
  }

  @Test
  public void topUnmarshalMinimumAnnotations() throws IOException {
    ImmutableMinimumAnnotationsMapped mapped = ImmutableMinimumAnnotationsMapped.builder().a("a").addB(1, 2).build();
    String json = OBJECT_MAPPER.writeValueAsString(mapped);
    check(OBJECT_MAPPER.readValue(json, ImmutableMinimumAnnotationsMapped.class)).is(mapped);
  }

  @Test
  public void minimumMarshaledPropertyNames() throws IOException {
    String originalSampleJson = "{\"A\":\"a\", \"B\": [1, 2]}";
    ImmutableMinimumAnnotationsMapped mapped =
        OBJECT_MAPPER.readValue(originalSampleJson, ImmutableMinimumAnnotationsMapped.class);
    String json = OBJECT_MAPPER.writeValueAsString(mapped);
    check(OBJECT_MAPPER.readValue(json, ImmutableMinimumAnnotationsMapped.class)).is(mapped);
  }

  @Test
  public void topLevelMarshalUnmarshal() throws IOException {
    ImmutableSampleJacksonMapped mapped = ImmutableSampleJacksonMapped.builder().a("a").addB(1, 2).build();
    String json = OBJECT_MAPPER.writeValueAsString(mapped);
    check(OBJECT_MAPPER.readValue(json, ImmutableSampleJacksonMapped.class)).is(mapped);
  }

  @Test
  public void nestedMarshalUnmarshal() throws IOException {
    Wrapper wrapper = new Wrapper();
    wrapper.mapped = ImmutableSampleJacksonMapped.builder().a("a").addB(1, 2).build();

    String json = OBJECT_MAPPER.writeValueAsString(wrapper);
    check(OBJECT_MAPPER.readValue(json, Wrapper.class).mapped).is(wrapper.mapped);
  }

  @Test
  public void jacksonRoundtrip() throws IOException {
    SampleJacksonMapped.RegularPojo pojo = new SampleJacksonMapped.RegularPojo();
    pojo.x = 1;
    pojo.y = 2;

    ImmutableSampleJacksonMapped original = ImmutableSampleJacksonMapped.builder()
        .a("a")
        .pojo(pojo)
        .build();

    String json = OBJECT_MAPPER.writeValueAsString(original);
    SampleJacksonMapped value = OBJECT_MAPPER.readValue(json, SampleJacksonMapped.class);
    check(value).is(original);
    check(value).not().same(original);
  }

  @Test
  public void abstractUnmarshalByAnnotation() throws IOException {
    ImmutableSampleJacksonMapped original = ImmutableSampleJacksonMapped.builder().a("a").build();
    String json = OBJECT_MAPPER.writeValueAsString(original);
    SampleJacksonMapped value = OBJECT_MAPPER.readValue(json, SampleJacksonMapped.class);
    check(value).is(original);
    check(value).not().same(original);
  }
}
