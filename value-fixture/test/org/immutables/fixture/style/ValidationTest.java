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
    check(!nv.z());
    check(nv.i()).is(0);

    nv = ImmutableNoValidation.of(null, null, true, 0);
    check(nv.a()).isNull();
    check(nv.b()).isNull();
    check(nv.z());

    nv = nv.withA(null).withB(null);

    check(nv.a()).isNull();
    check(nv.b()).isNull();
  }
}
