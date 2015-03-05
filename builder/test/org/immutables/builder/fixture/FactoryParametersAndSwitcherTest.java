package org.immutables.builder.fixture;

import org.immutables.builder.fixture.Factory1Builder;
import org.immutables.builder.fixture.Factory2Builder;
import org.immutables.builder.fixture.Factory3Builder;
import org.immutables.builder.fixture.Factory4Builder;
import org.immutables.builder.fixture.Factory5Builder;
import java.lang.annotation.RetentionPolicy;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class FactoryParametersAndSwitcherTest {

  @Test
  public void parameters() {

    check(new Factory1Builder(null)
        .theory(1)
        .reality("1")
        .build()).is("1 != 1, null");

    check(new Factory2Builder(2, "2").build()).is("2 != 2");

    check(Factory3Builder.newBuilder(3).reality("3").build()).is("3 != 3");
  }

  @Test
  public void switcher() {

    check(Factory4Builder.newBuilder(4)
        .runtimePolicy()
        .sourcePolicy()
        .classPolicy()
        .build()).is("" + RetentionPolicy.CLASS + 4);

    check(Factory4Builder.newBuilder(42)
        .sourcePolicy()
        .runtimePolicy()
        .build()).is("" + RetentionPolicy.RUNTIME + 42);

    try {
      Factory4Builder.newBuilder(44).build();
      check(false);
    } catch (IllegalStateException ex) {
    }
  }

  @Test
  public void switcherDefaults() {
    check(Factory5Builder.newBuilder().build()).is("" + RetentionPolicy.SOURCE.toString());
    check(Factory5Builder.newBuilder().runtimePolicy().build()).is("" + RetentionPolicy.RUNTIME);
  }
}
