package org.immutables.fixture.generics;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.immutables.value.Value;

// Compilation test (and warning for the raw types)
@Value.Immutable
@SuppressWarnings("rawtypes")
@Value.Style(strictBuilder = true)
public interface RawType {
  Set set();

  Map map();

  static void use() {
    ImmutableRawType.builder()
        .set(Collections.emptySet())
        .map(Collections.emptyMap())
        .build();
  }
}
