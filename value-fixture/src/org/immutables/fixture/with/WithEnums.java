package org.immutables.fixture.with;

import org.immutables.value.Value;

import java.math.RoundingMode;
import java.util.Optional;

import javax.annotation.Nullable;

@Value.Immutable
public abstract class WithEnums {
  public abstract RoundingMode getRoundingMode();
  @Nullable
  public abstract RoundingMode getNullableRoundingMode();
  public abstract Optional<RoundingMode> getMaybeRoundingMode();
}
