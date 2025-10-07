package org.immutables.datatype;

import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@MetaModel
@Enclosing
interface Maybe<T> {
  @Immutable(builder = false)
  interface Some<T> extends Maybe<T> {
    @Parameter
    T value();
  }

  @Immutable(builder = false, singleton = true)
  interface None<T> extends Maybe<T> {}
}
