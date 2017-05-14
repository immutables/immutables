package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import nonimmutables.jackson.JacksonMeta;
import org.immutables.value.Value;

@JacksonMeta
@Value.Immutable
public interface JacksonUsingMeta {
  @JacksonMeta.Any
  Map<String, JsonNode> any();
  @JacksonMeta.X
  int y();
}
