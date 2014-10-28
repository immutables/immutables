package org.immutables.fixture.jackson;

import org.immutables.fixture.jackson.ImmutableSampleJacksonMapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class ObjectMappedTest {
  ObjectMapper objectMapper = new ObjectMapper();

  public static class Wrapper {
    public ImmutableSampleJacksonMapped mapped;
  }

  @Test
  public void topLevelMarshalUnmarshal() throws IOException {
    ImmutableSampleJacksonMapped mapped = ImmutableSampleJacksonMapped.builder().a("a").addB(1, 2).build();
    String json = objectMapper.writeValueAsString(mapped);
    check(objectMapper.readValue(json, ImmutableSampleJacksonMapped.class)).is(mapped);
  }

  @Test
  public void nestedMarshalUnmarshal() throws IOException {
    Wrapper wrapper = new Wrapper();
    wrapper.mapped = ImmutableSampleJacksonMapped.builder().a("a").addB(1, 2).build();

    String json = objectMapper.writeValueAsString(wrapper);
    check(objectMapper.readValue(json, Wrapper.class).mapped).is(wrapper.mapped);
  }

  @Test
  public void abstractUnmarshalByAnnotation() throws IOException {
    ImmutableSampleJacksonMapped original = ImmutableSampleJacksonMapped.builder().a("a").build();
    String json = objectMapper.writeValueAsString(original);
    SampleJacksonMapped value = objectMapper.readValue(json, SampleJacksonMapped.class);
    check(value).is(original);
    check(value).not().same(original);
  }
}
