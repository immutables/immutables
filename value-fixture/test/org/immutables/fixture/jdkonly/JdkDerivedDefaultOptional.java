package org.immutables.fixture.jdkonly;

import static org.immutables.check.Checkers.check;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class JdkDerivedDefaultOptional {
  @Test
  public void derivedDefaultOptional() {
    DerivedDefaultOptional derived = ImmutableDerivedDefaultOptional.builder().a(1).b(2).build();
    check(derived.derived()).is(-1);

    //b is 5 as default
    DerivedDefaultOptional derived2 = ImmutableDerivedDefaultOptional.builder().a(1).build();
    check(derived2.derived()).is(-4);

    //derived uses 0 for b if set to Optional.empty()
    DerivedDefaultOptional derived3 = ImmutableDerivedDefaultOptional.builder().a(1).b(Optional.empty()).build();
    check(derived3.derived()).is(1);
  }
}
