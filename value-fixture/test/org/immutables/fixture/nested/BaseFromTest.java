package org.immutables.fixture.nested;

import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class BaseFromTest {
  @Test
  public void from() {
    BaseFrom baseFrom = new BaseFrom() {
      @Override
      public boolean isA() {
        return false;
      }

      @Override
      public String getB() {
        return "";
      }
    };
    ImmutableSub sub = ImmutableSub.builder()
        .from(baseFrom)
        .c("*")
        .build();

    check(sub.getB()).is("");
    check(sub.getC()).is("*");
    check(!sub.isA());
  }

  @Test
  public void complicatedFrom() {
    ImmutableAB ab = ImmutableAB.builder()
        .a(1)
        .addB("a", "b")
        .c(3)
        .build();

    ImmutableAB copiedAb = ImmutableAB.builder()
        .from(ab)
        .build();

    check(copiedAb).is(ab);
  }
}
