package org.immutables.fixture.generics;

import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
interface GenericsAsItIs<T> {}

@Value.Immutable
public interface UsingNotYetGenerateGenerics<T> {
  ImmutableGenericsAsItIs<String> notYetGenerated();

  ImmutableB<T> andThisToo();

  List<ImmutableGenericsAsItIs<String>> notYetInCollection();
}
