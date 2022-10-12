package org.immutables.fixture.with;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

public class WithUnaryOperatorTest {
  @Test
  public void mapAttributes() {
    ImmutableWithUnaryOperator m = ImmutableWithUnaryOperator.builder()
        .a(1)
        .b("b")
        .addL("x", "y", "z")
        .o("**")
        .build();

    ImmutableWithUnaryOperator t = m.mapA(a -> a + 1)
        .mapB(b -> "[" + b + "]")
        .mapL(String::toUpperCase)
        .mapO(o -> o.replace('*', '_'));

    check(t.a()).is(2);
    check(t.b()).is("[b]");
    check(t.l()).isOf("X", "Y", "Z");
    check(t.o()).is(Optional.of("__"));
  }

  @Test
  public void mapEmptyReturningThis() {
    ImmutableWithUnaryOperator m = ImmutableWithUnaryOperator.builder()
        .a(1)
        .b("b")
        .build();

    ImmutableWithUnaryOperator t = m.mapO(o -> o + "!!!")
        .mapL(l -> l + "???")
        .mapI(i -> ++i)
        .mapD(d -> d + 0.1)
        .mapOi(oi -> oi * 10);

    check(t).same(m);
  }
}
