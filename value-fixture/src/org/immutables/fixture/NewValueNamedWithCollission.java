package org.immutables.fixture;

import org.immutables.value.Value.Immutable;

// compilation test for new value names
@Immutable
public interface NewValueNamedWithCollission {
  String getNewValue();

  String getOldValue();

  default boolean isDifferent() {
    return !getNewValue().equals(getOldValue());
  }
}
