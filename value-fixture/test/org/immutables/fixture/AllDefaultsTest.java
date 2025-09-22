package org.immutables.fixture;

import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

class AllDefaultsTest {
  @Test void keepAllDefaults() {
    AllDefaults withAllValuesChanged = ImmutableAllDefaults.builder().build();

    check(withAllValuesChanged.aChar()).is('a');
    check(withAllValuesChanged.aDouble()).is(1.0);
    check(withAllValuesChanged.aFloat()).is(0.1f);
    check(withAllValuesChanged.aInt()).is(0);
    check(withAllValuesChanged.aLong()).is(1L);
    check(withAllValuesChanged.aString()).is("foo");
  }
  @Test void overwriteAllDefaults() {
    AllDefaults withAllValuesChanged = ImmutableAllDefaults.builder()
        .aBoolean(true)
        .aChar('b')
        .aDouble(2.0)
        .aFloat(3.0f)
        .aInt(1)
        .aLong(2)
        .aString("bar")
        .build();

    check(withAllValuesChanged.aChar()).is('b');
    check(withAllValuesChanged.aDouble()).is(2.0);
    check(withAllValuesChanged.aFloat()).is(3.0f);
    check(withAllValuesChanged.aInt()).is(1);
    check(withAllValuesChanged.aLong()).is(2L);
    check(withAllValuesChanged.aString()).is("bar");
  }

  @Test void keepAllUnsafeDefaults() {
    AllDefaultsSafeUnsafe withAllValuesChanged = ImmutableAllDefaultsSafeUnsafe.builder().build();

    check(withAllValuesChanged.aChar()).is('a');
    check(withAllValuesChanged.aDouble()).is(1.0);
    check(withAllValuesChanged.aFloat()).is(0.1f);
    check(withAllValuesChanged.aInt()).is(0);
    check(withAllValuesChanged.aLong()).is(1L);
    check(withAllValuesChanged.aString()).is("foo");
  }
  @Test void overwriteAllUnsafeDefaults() {
    AllDefaultsSafeUnsafe withAllValuesChanged = ImmutableAllDefaultsSafeUnsafe.builder()
        .aBoolean(true)
        .aChar('b')
        .aDouble(2.0)
        .aFloat(3.0f)
        .aInt(1)
        .aLong(2)
        .aString("bar")
        .build();

    check(withAllValuesChanged.aChar()).is('b');
    check(withAllValuesChanged.aDouble()).is(2.0);
    check(withAllValuesChanged.aFloat()).is(3.0f);
    check(withAllValuesChanged.aInt()).is(1);
    check(withAllValuesChanged.aLong()).is(2L);
    check(withAllValuesChanged.aString()).is("bar");
  }
}
