package org.immutables.fixture.couse;

import com.google.common.base.Optional;
import org.immutables.value.Value;

@Value.Immutable
interface AbstractB {
  A a();

  Optional<A> aO();

  default void use() {
    B b = B.builder()
        .a(A.builder().build())
        .aO(Optional.absent())
        .build();

    b.toString();
  }
}
