/*
   Copyright 2014-2018 Immutables Authors and Contributors

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
package org.immutables.fixture.jackson3;

import static org.immutables.check.Checkers.check;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.guava.GuavaModule;

public class ObjectMappedTest {
  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
    .addModule(new GuavaModule())
    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    .build();

  public static class Wrapper {
    public ImmutableSampleJacksonMapped mapped;
  }

  @Test
  public void jsonPropertyName() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Foo foo = mapper.readValue("{\"bar\": true}", Foo.class);
    check(foo.isBar());
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
  public void minimumIgnoreUnknownNames() {
    String originalSampleJson = "{\"A\":\"a\", \"B\": [1, 2], \"Z\": false}";
    ImmutableMinimumAnnotationsMapped mapped =
        OBJECT_MAPPER.readValue(originalSampleJson, ImmutableMinimumAnnotationsMapped.class);
    String json = OBJECT_MAPPER.writeValueAsString(mapped);
    check(OBJECT_MAPPER.readValue(json, ImmutableMinimumAnnotationsMapped.class)).is(mapped);
  }

  @Test
  public void topLevelMarshalUnmarshal() {
    ImmutableSampleJacksonMapped mapped = ImmutableSampleJacksonMapped.builder().a("a").addB(1, 2).build();
    String json = OBJECT_MAPPER.writeValueAsString(mapped);
    check(OBJECT_MAPPER.readValue(json, ImmutableSampleJacksonMapped.class)).is(mapped);
  }

  @Test
  public void nestedMarshalUnmarshal() {
    Wrapper wrapper = new Wrapper();
    wrapper.mapped = ImmutableSampleJacksonMapped.builder().a("a").addB(1, 2).build();

    String json = OBJECT_MAPPER.writeValueAsString(wrapper);
    check(OBJECT_MAPPER.readValue(json, Wrapper.class).mapped).is(wrapper.mapped);
  }

  @Test
  public void jacksonRoundtrip() {
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
  public void abstractUnmarshalByAnnotation() {
    ImmutableSampleJacksonMapped original = ImmutableSampleJacksonMapped.builder().a("a").build();
    String json = OBJECT_MAPPER.writeValueAsString(original);
    SampleJacksonMapped value = OBJECT_MAPPER.readValue(json, SampleJacksonMapped.class);
    check(value).is(original);
    check(value).not().same(original);
  }

  @Test
  public void includeNonEmpty() {
    String json = "{}";
    OptionIncludeNonEmpty value = OBJECT_MAPPER.readValue(json, OptionIncludeNonEmpty.class);
    check(OBJECT_MAPPER.writeValueAsString(value)).is(json);
  }

  @Test
  public void includeNonEmptyWithConstructor() throws Exception {
    String json = "{}";
    OptionIncludeNonEmptyWithConstructor value =
        OBJECT_MAPPER.readValue(json, OptionIncludeNonEmptyWithConstructor.class);
    check(OBJECT_MAPPER.writeValueAsString(value)).is(json);
  }

}
