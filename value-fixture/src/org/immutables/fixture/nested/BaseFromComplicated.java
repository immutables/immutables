package org.immutables.fixture.nested;

import org.immutables.value.Value;
import java.util.List;

public interface BaseFromComplicated {

  interface A {
    int a();

    List<String> b();
  }

  interface AA {
    int a();
  }

  interface B extends AA {
    List<String> b();
  }

  @Value.Immutable
  interface AB extends A, B {
    @Override
    int a();

    int c();

    @Override
    List<String> b();
  }

  @Value.Modifiable
  @Value.Immutable
  interface AAA extends AA, A {
    @Override
    int a();

    @Override
    List<String> b();
  }
}
