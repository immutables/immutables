package org.immutables.fixture.generics;

import org.immutables.value.Value;

@Value.Immutable
interface GenericsAsItIs<T> {}

@Value.Immutable
public interface UsingNotYetGenerateGenerics<T> {
  ImmutableGenericsAsItIs<String> notYetGenerated();

  ImmutableB<T> andThisToo();
}
