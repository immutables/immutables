package org.immutables.fixture.style;

import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class ValidationTest {
  @Test
  public void noValidation() {
    ImmutableNoValidation nv = ImmutableNoValidation.builder()
        .a(null).build();

    check(nv.a()).isNull();
    check(nv.b()).isNull();

    nv = ImmutableNoValidation.of(null, null);
    check(nv.a()).isNull();
    check(nv.b()).isNull();

    nv = nv.withA(null).withB(null);

    check(nv.a()).isNull();
    check(nv.b()).isNull();
  }
}
