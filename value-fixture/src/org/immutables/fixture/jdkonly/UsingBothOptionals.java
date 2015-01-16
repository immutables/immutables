package org.immutables.fixture.jdkonly;

import java.util.Objects;
import com.google.common.base.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface UsingBothOptionals {
  Optional<Integer> v1();

  java.util.Optional<Integer> v2();

  class Use {
    void use() {
      ImmutableUsingBothOptionals value =
          ImmutableUsingBothOptionals.builder()
              .v1(1)
              .v2(2)
              .build();

      Objects.equals(value.v1().get(), value.v2().get());
    }
  }
}
