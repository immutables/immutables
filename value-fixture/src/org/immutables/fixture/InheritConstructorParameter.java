package org.immutables.fixture;

import org.immutables.value.Value;

interface InheritConstructorParameter {

  abstract class HasParam {
    @Value.Parameter
    abstract int i();
  }

  @Value.Immutable
  abstract class ExtendsIt extends HasParam {}

  default void use() {
    ImmutableExtendsIt.of(1);
  }
}
