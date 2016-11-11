package org.immutables.fixture.nullable;

import org.eclipse.jdt.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
interface NullableTypeUseJdtAccepted {
  @Nullable
  Integer i1();

  @Nullable
  Long l2();
}
