package org.immutables.fixture;

import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class SafeInitTest {
  @Test
  public void cycles() {
    try {
      ImmutableSafeInitIface.builder().build();
      check(false);
    } catch (IllegalStateException ex) {
      check(ex.getMessage()).contains("[b, a, c]");
    }
  }

  @Test
  public void resolved() {
    check(ImmutableSafeInitIface.builder().a(1).build().c()).is(1);
    check(ImmutableSafeInitIface.builder().b(2).build().a()).is(2);
    check(ImmutableSafeInitIface.builder().c(3).build().b()).is(3);
  }

  @Test
  public void singleton() {
    ImmutableSafeInitSingl singl = ImmutableSafeInitSingl.of();
    check(singl.a()).is(1);
    check(singl.b()).is(2);

    ImmutableSafeInitSingl nonSingleton = ImmutableSafeInitSingl.builder()
        .a(0)
        .build();

    check(nonSingleton.b()).is(1);
  }

  @Test
  public void unsafeOrdering() {
    ImmutableSafeInitAclass obj = ImmutableSafeInitAclass.builder()
        .c(1)
        .build();

    check(obj.a()).not(1);
    check(obj.b()).not(1);
  }
}
