package org.immutables.fixture.encoding.defs;

import java.util.OptionalInt;
import org.immutables.encode.Encoding;

@Encoding
class CompactOptionalInt {
  @Encoding.Impl(virtual = true)
  private OptionalInt opt;

  private final int value = opt.orElse(0);
  private final boolean present = opt.isPresent();

  @Encoding.Expose
  OptionalInt get() {
    return present
        ? OptionalInt.of(value)
        : OptionalInt.empty();
  }
}
