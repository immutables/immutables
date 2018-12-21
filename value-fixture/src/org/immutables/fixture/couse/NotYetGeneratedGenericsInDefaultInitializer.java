package org.immutables.fixture.couse;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.immutables.value.Value;

@Value.Style // reset
@Value.Immutable
interface NotYetGeneratedGenericsInDefaultInitializer {
  @Value.Style // reset
  @Value.Immutable
  interface ToBeGenX<X, Y> {}

  @Value.Default
  default Supplier<Stream<ImmutableToBeGenX<?, ?>>> getEventSupplier() {
    return Stream::empty;
  }
}
