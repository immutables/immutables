package org.immutables.fixture.nested;

import com.google.common.base.Optional;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.value.Value;

@Value.Nested
@Value.Transformer
@GenerateMarshaler
class GroupedClasses {
  interface Other {}

  @Value.Immutable
  interface NestedOne extends Other {
    Optional<Other> other();

    int attribute();
  }
}
