package org.immutables.fixture.with;

import org.junit.Test;

import java.math.RoundingMode;

import static org.immutables.check.Checkers.check;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WithEnumsTest {
  private static final ImmutableWithEnums TEST_IMMUTABLE = ImmutableWithEnums.builder()
          .roundingMode(RoundingMode.CEILING)
          .maybeRoundingMode(RoundingMode.HALF_DOWN)
          .nullableRoundingMode(RoundingMode.UNNECESSARY)
          .build();

  @Test
  public void doNotAllowNullEnumValueInNonNullableWith() {
    assertThrows(NullPointerException.class, () -> TEST_IMMUTABLE.withRoundingMode(null));
  }

  @Test
  public void allowNullEnumValueInNullableWith() {
    ImmutableWithEnums withNullableRoundingMode = TEST_IMMUTABLE.withNullableRoundingMode(null);
    check(withNullableRoundingMode.getNullableRoundingMode()).isNull();
  }
}
