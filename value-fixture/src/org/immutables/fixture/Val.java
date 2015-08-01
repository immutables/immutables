package org.immutables.fixture;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
abstract class ValDerivedCollection {
  @Value.Derived
  Set<Integer> available() {
    return ImmutableSet.of(1);
  }
}
