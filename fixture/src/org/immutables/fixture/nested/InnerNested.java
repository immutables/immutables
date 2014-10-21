package org.immutables.fixture.nested;

import org.immutables.json.Json;
import org.immutables.value.Value;

@Value.Nested
@Value.Immutable
public interface InnerNested {
  @Value.Immutable
  @Json.Marshaled
  abstract static class Inner {}

  @Value.Immutable
  @Json.Marshaled
  interface Nested {}
}
