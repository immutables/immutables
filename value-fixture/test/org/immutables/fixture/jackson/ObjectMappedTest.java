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
package org.immutables.fixture.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

public class ObjectMappedTest {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  {
    OBJECT_MAPPER.registerModule(new GuavaModule());
  }

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
  public void minimumIgnoreUnknownNames() throws IOException {
    String originalSampleJson = "{\"A\":\"a\", \"B\": [1, 2], \"Z\": false}";
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

  @Test
  public void includeNonEmpty() throws Exception {
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

  @Test
  public void propertyOrder() throws Exception {
    String json = "[0.1,1.2,2.3]";
    GeoPoint value = OBJECT_MAPPER.readValue(json, GeoPoint.class);
    check(OBJECT_MAPPER.writeValueAsString(value)).is(json);
  }

  @Test
  public void propertyOrder2() throws Exception {
    String json = "{\"lat\":0.1,\"lon\":2.3}";
    GeoPoint2 value = OBJECT_MAPPER.readValue(json, GeoPoint2.class);
    check(OBJECT_MAPPER.writeValueAsString(value)).is(json);
  }

  @Test
  public void anyGetterSetter() throws Exception {
    String json = "{\"A\":1,\"B\":true}";
    String jsonRegex = "\\{(?:\"A\":1,\"B\":true|\"B\":true,\"A\":1)}";
    AnyGetterSetter value = OBJECT_MAPPER.readValue(json, AnyGetterSetter.class);
    check(OBJECT_MAPPER.writeValueAsString(value)).matches(jsonRegex);
  }

  @Test
  public void anyGetterInBuilderSetter() throws Exception {
    String json = "{\"A\":1,\"B\":true}";
    AnyGetterInBuilder value = OBJECT_MAPPER.readValue(json, AnyGetterInBuilder.class);
    check(OBJECT_MAPPER.writeValueAsString(value)).is(json);
  }

  @Test
  public void jacksonMetaAnnotated() throws Exception {
    String json = "{\"X\":1,\"A\":1,\"B\":true}";
    String jsonRegex = "\\{(?:\"X\":1,(?:\"A\":1,\"B\":true|\"B\":true,\"A\":1)|"
            + "\"B\":true,(?:\"X\":1,\"A\":1|\"A\":1,\"X\":1)|"
            + "\"A\":1,(?:\"B\":true,\"X\":1|\"X\":1,\"B\":true))}";
    ImmutableJacksonUsingMeta value = OBJECT_MAPPER.readValue(json, ImmutableJacksonUsingMeta.class);
    check(OBJECT_MAPPER.writeValueAsString(value)).matches(jsonRegex);
  }

  @Test
  public void keywordNames() throws Exception {
    String json = "{\"long\":111,\"default\":true}";
    String jsonRegex = "\\{(?:\"long\":111,\"default\":true|\"default\":true,\"long\":111)}";
    KeywordNames value = OBJECT_MAPPER.readValue(json, KeywordNames.class);
    check(OBJECT_MAPPER.writeValueAsString(value)).matches(jsonRegex);
  }

  @Test
  public void lazyAttribute() throws Exception {
    String json = "{\"a\":1}";
    LazyAttributesSafe value = OBJECT_MAPPER.readValue(json, LazyAttributesSafe.class);
    check(value.getA()).is(1);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void noAnnotationsWorks() throws Exception {
    check(ImmutableJacksonMappedWithNoAnnotations.Json.class.getAnnotation(JsonDeserialize.class)).isNull();
    String json = "{\"someString\":\"xxx\"}";
    ImmutableJacksonMappedWithNoAnnotations value =
        OBJECT_MAPPER.readValue(json, ImmutableJacksonMappedWithNoAnnotations.class);
    check(OBJECT_MAPPER.writeValueAsString(value)).is(json);
  }

  @Test
  public void packageHiddenInsideBuilder() throws Exception {
    String json = "{\"strings\":[\"asd\"]}";
    PackageHidden example = OBJECT_MAPPER.readValue(json, PackageHidden.class);
    check(example.getStrings()).isOf("asd");
  }

  @Test
  public void customBuilderDeserialize() throws Exception {
    String json = "{\"a\":1,\"s\":\"abc\",\"l\":[true,false],\"unknownShouldBeIgnored\":1}";
    CustomBuilderDeserialize o = OBJECT_MAPPER.readValue(json, CustomBuilderDeserialize.class);
    check(o.a()).is(1);
    check(o.s()).is("abc");
    check(o.l()).isOf(true, false);

    check(OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(o), CustomBuilderDeserialize.class)).is(o);
  }

  @Test
  public void jsonValueBuilderRoundTrip() throws Exception {
    String json = "[\"a\"]";

    List<JsonValueCreator> values =
        OBJECT_MAPPER.readValue(json, new TypeReference<List<JsonValueCreator>>() {});

    check(OBJECT_MAPPER.writeValueAsString(values)).is(json);
  }

  @Test
  public void jsonValueConstructorRoundtrip() throws Exception {
    String json = "[true]";

    List<JsonValueCreator.Constructor> values =
        OBJECT_MAPPER.readValue(json, new TypeReference<List<JsonValueCreator.Constructor>>() {});

    check(OBJECT_MAPPER.writeValueAsString(values)).is(json);
  }

  @Test
  public void jsonValueSingletonRoundtrip() throws Exception {
    String json = "[1.4]";

    List<JsonValueCreator.Singleton> values =
        OBJECT_MAPPER.readValue(json, new TypeReference<List<JsonValueCreator.Singleton>>() {});

    check(OBJECT_MAPPER.writeValueAsString(values)).is(json);
  }

  @Test
  public void jsonStagedEntityRoundtrip() throws Exception {
    String json = "{\"required\":\"id\",\"optional\":null,\"optionalPrimitive\":null}";

    StagedEntity nullable =
        OBJECT_MAPPER.readValue(json, StagedEntity.class);

    check(OBJECT_MAPPER.writeValueAsString(nullable)).is("{\"required\":\"id\",\"optional\":\"default\",\"optionalPrimitive\":false}");
  }
}
