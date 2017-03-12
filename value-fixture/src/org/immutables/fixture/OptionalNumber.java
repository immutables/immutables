package org.immutables.fixture;

import com.google.common.base.Optional;
import java.math.BigDecimal;
import org.immutables.value.Value;

@Value.Immutable
public interface OptionalNumber {
  Optional<BigDecimal> bigDecimal();
}
