package org.immutables.fixture.annotation;

import nonimmutables.A1;
import nonimmutables.A2;
import nonimmutables.B1;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class PassAnnotationTest {
  @Test
  public void passAnnotations() {
    check(ImmutableValForPass.class.getAnnotation(A1.class)).notNull();
    check(ImmutableValForPass.class.getAnnotation(A2.class)).notNull();
    check(ImmutableValForPass.class.getAnnotation(B1.class)).isNull();
  }
}
