package org.immutables.fixture;

import java.math.BigDecimal;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface OptionalNumber {
  Optional<BigDecimal> bigDecimal();
}
