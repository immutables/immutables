package org.immutables.fixture.nested;

import static org.immutables.check.Checkers.*;
import org.junit.Test;

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
}
