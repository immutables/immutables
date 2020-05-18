package org.immutables.data;

import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Data
@Enclosing
interface Opt<T> {
  @Immutable(builder = false)
  interface Some<T> extends Opt<T> {
    @Parameter
    T value();
  }

  @Immutable(builder = false, singleton = true)
  interface None<T> extends Opt<T> {}
}
