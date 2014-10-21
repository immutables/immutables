package org.immutables.fixture.nested;

import com.google.common.base.Optional;
import org.immutables.json.Json;
import org.immutables.value.Value;

@Value.Nested
@Value.Transformer
@Json.Marshaled
class GroupedClasses {
  interface Other {}

  @Value.Immutable
  interface NestedOne extends Other {
    Optional<Other> other();

    int attribute();
  }
}
