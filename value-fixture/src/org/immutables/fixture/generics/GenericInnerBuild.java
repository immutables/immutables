package org.immutables.fixture.generics;

import org.immutables.value.Value;

@Value.Immutable
interface GenericInnerBuild<T, X> {
  interface Builder<T> {

  }
}
