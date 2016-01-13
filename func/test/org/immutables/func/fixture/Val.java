package org.immutables.func.fixture;

import org.immutables.func.Functional;
import org.immutables.value.Value;

@Value.Immutable
@Functional
public interface Val {

  boolean isEmpty();

  String getName();

  int age();
}
