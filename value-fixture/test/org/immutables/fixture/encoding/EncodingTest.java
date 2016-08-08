package org.immutables.fixture.encoding;

import static org.immutables.check.Checkers.check;
import com.google.common.collect.ImmutableMap;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.IOException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class EncodingTest {
  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new GuavaModule());

  @Test
  public void optMap() throws Exception {
    UseOptionalMap<Boolean> value =
        objectMapper.readValue("{\"maybeMap\":{\"a\":true}}", new TypeReference<UseOptionalMap<Boolean>>() {});

    check(value).is(ImmutableUseOptionalMap.<Boolean>builder()
        .putMaybeMap("a", true)
        .build());

    check(value.maybeMap()).isOf(ImmutableMap.of("a", true));
  }
}
