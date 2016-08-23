package org.immutables.fixture.encoding.defs;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.immutables.encode.Encoding;

@Encoding
class CompactOptionalDouble {
  @Encoding.Impl(virtual = true)
  private OptionalDouble opt;

  private final double value = opt.orElse(0);
  private final boolean present = opt.isPresent();

  @Encoding.Of
  static OptionalDouble from(Object ddd) {
    if (Double.isNaN((Double) ddd)) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of((Double) ddd);
  }

  @Encoding.Expose
  OptionalDouble get() {
    return present
        ? OptionalDouble.of(value)
        : OptionalDouble.empty();
  }
}
