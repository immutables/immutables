package org.immutables.func.fixture;

import javax.annotation.Nullable;
import org.immutables.func.Functional;
import org.immutables.value.Value;

@Value.Immutable
@Functional
public interface Val {

  boolean isEmpty();

  String getName();

  @Nullable
  Number nullable();

  int age();
}
