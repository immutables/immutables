package org.immutables.fixture.annotation;

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
