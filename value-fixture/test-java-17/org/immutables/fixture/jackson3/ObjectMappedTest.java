/*
   Copyright 2014-2025 Immutables Authors and Contributors

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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.guava.GuavaModule;

class ObjectMappedTest {
  private static final ObjectMapper mapper = JsonMapper.builder()
    .addModule(new GuavaModule())
    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    .build();

  static class Wrapper {
    public ImmutableSampleJacksonMapped mapped;
  }

  @Test void jsonPropertyName() {
    var mapper = new ObjectMapper();
    Foo foo = mapper.readValue("{\"bar\": true}", Foo.class);
    check(foo.isBar());
  }

  @Test void constructorMapping() {
    var originalSampleJson = "{\"X\":1, \"bal\": \"V\"}";
    var mapped = mapper.readValue(originalSampleJson, ConstructorJacksonMapped.class);
    String json = mapper.writeValueAsString(mapped);
    check(mapper.readValue(json, ConstructorJacksonMapped.class)).is(mapped);
  }

  @Test void topUnmarshalMinimumAnnotations() {
    var mapped = ImmutableMinimumAnnotationsMapped.builder().a("a").addB(1, 2).build();
    var json = mapper.writeValueAsString(mapped);
    check(mapper.readValue(json, ImmutableMinimumAnnotationsMapped.class)).is(mapped);
  }

  @Test void minimumMarshaledPropertyNames() {
    var originalSampleJson = "{\"A\":\"a\", \"B\": [1, 2]}";
    var mapped = mapper.readValue(originalSampleJson, ImmutableMinimumAnnotationsMapped.class);
    String json = mapper.writeValueAsString(mapped);
    check(mapper.readValue(json, ImmutableMinimumAnnotationsMapped.class)).is(mapped);
  }

  @Test void minimumIgnoreUnknownNames() {
    var originalSampleJson = "{\"A\":\"a\", \"B\": [1, 2], \"Z\": false}";
    var mapped =mapper.readValue(originalSampleJson, ImmutableMinimumAnnotationsMapped.class);
    var json = mapper.writeValueAsString(mapped);
    check(mapper.readValue(json, ImmutableMinimumAnnotationsMapped.class)).is(mapped);
  }

  @Test void topLevelMarshalUnmarshal() {
    var mapped = ImmutableSampleJacksonMapped.builder()
        .a("a")
        .addB(1, 2)
        .build();
    var json = mapper.writeValueAsString(mapped);
    check(mapper.readValue(json, ImmutableSampleJacksonMapped.class)).is(mapped);
  }

  @Test void nestedMarshalUnmarshal() {
    Wrapper wrapper = new Wrapper();
    wrapper.mapped = ImmutableSampleJacksonMapped.builder().a("a").addB(1, 2).build();

    String json = mapper.writeValueAsString(wrapper);
    check(mapper.readValue(json, Wrapper.class).mapped).is(wrapper.mapped);
  }

  @Test void jacksonRoundtrip() {
    var pojo = new SampleJacksonMapped.RegularPojo();
    pojo.x = 1;
    pojo.y = 2;

    var original = ImmutableSampleJacksonMapped.builder()
        .a("a")
        .pojo(pojo)
        .build();

    var json = mapper.writeValueAsString(original);
    var value = mapper.readValue(json, SampleJacksonMapped.class);
    check(value).is(original);
    check(value).not().same(original);
  }

  @Test void abstractUnmarshalByAnnotation() {
    var original = ImmutableSampleJacksonMapped.builder().a("a").build();
    var json = mapper.writeValueAsString(original);
    var value = mapper.readValue(json, SampleJacksonMapped.class);
    check(value).is(original);
    check(value).not().same(original);
  }

  @Test void includeNonEmpty() {
    var json = "{}";
    var value = mapper.readValue(json, OptionIncludeNonEmpty.class);
    check(mapper.writeValueAsString(value)).is(json);
  }

  @Test void includeNonEmptyWithConstructor() throws Exception {
    var json = "{}";
    var value = mapper.readValue(json, OptionIncludeNonEmptyWithConstructor.class);
    check(mapper.writeValueAsString(value)).is(json);
  }

  @Test void renamedFieldDeserializedWithBuilder() throws Exception {
    var json = """
            { "start_date_time": "2020-05-14T19:35+0200" }
            """;
    var value = mapper.readValue(json, HavingRenamedField.class);
    check(value.getStartDateTime()).notNull();
  }
}
