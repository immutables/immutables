package org.immutables.fixture.jdkonly;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public abstract class DefaultArray {
  @Value.Default
  int[] prop() {
    return new int[0];
  }

  @Value.Default
  ImmutableSet<Integer> ints() {
    return ImmutableSortedSet.of();
  }
}
