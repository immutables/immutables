package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import java.io.IOException;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class DefaultTypingTest {
  @Test
  public void roundtrip() throws IOException {
    final ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.enableDefaultTyping();
    objectMapper.registerModule(new GuavaModule());

    ImmutableOuterObject outer = ImmutableOuterObject.builder()
        .emptyObject(
            ImmutableEmptyObject
                .builder()
                .build())
        .build();

    String serialized = objectMapper.writeValueAsString(outer);

    check(objectMapper.readValue(serialized, ImmutableOuterObject.class)).is(outer);
  }
}
