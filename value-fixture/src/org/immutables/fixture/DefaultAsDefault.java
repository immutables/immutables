package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(defaultAsDefault = true)
public interface DefaultAsDefault {
  default int a() {
    return 1;
  }

  default int b() {
    return 2;
  }
}
