package org.immutables.fixture.jdkonly;

import org.immutables.gson.Gson;
import java.util.OptionalLong;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Objects;
import com.google.common.base.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Gson.TypeAdapters
public interface UsingAllOptionals {
  Optional<Integer> v1();

  java.util.Optional<Integer> v2();

  OptionalInt i1();

  OptionalLong l1();

  OptionalDouble d1();

  class Use {
    void use() {
      UsingAllOptionals value =
          ImmutableUsingAllOptionals.builder()
              .v1(1)
              .v2(2)
              .i1(OptionalInt.of(1))
              .d1(1.1)
              .l1(OptionalLong.empty())
              .build();

      Objects.equals(value.v1().get(), value.v2().get());
      Objects.hash(value.i1(), value.l1(), value.d1());
    }
  }
}
