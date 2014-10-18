package org.immutables.fixture.annotation;

import org.junit.Test;
import static org.immutables.check.Checkers.*;

@An(13)
public class AnnotationTest {

  @Test
  public void testName() {

    An runtime = getClass().getAnnotation(An.class);
    An immutable = ImmutableAn.of(13);

    check(immutable).is(runtime);
    check(runtime).is(immutable);
    check(ImmutableAn.of(1).annotationType() == An.class);
    check(immutable.hashCode()).is(runtime.hashCode());

    check(immutable.bees()).isOf(runtime.bees());
  }
}
