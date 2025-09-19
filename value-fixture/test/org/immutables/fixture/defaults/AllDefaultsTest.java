package org.immutables.fixture.defaults;

import org.immutables.check.Checkers;
import org.junit.jupiter.api.Test;

class AllDefaultsTest {

  @Test
  void testAllDefault() {
    AllDefaults withAllValuesChanged = ImmutableAllDefaults.builder()
        .aBoolean(true)
        .aChar('b')
        .aDouble(2.0)
        .aFloat(3.0f)
        .aInt(1)
        .aLong(2)
        .aString("bar")
        .build();

    Checkers.check(withAllValuesChanged.aChar()).is('b');
    Checkers.check(withAllValuesChanged.aDouble()).is(2.0);
    Checkers.check(withAllValuesChanged.aFloat()).is(3.0f);
    Checkers.check(withAllValuesChanged.aInt()).is(1);
    Checkers.check(withAllValuesChanged.aLong()).is(2L);
    Checkers.check(withAllValuesChanged.aString()).is("bar");
  }
}