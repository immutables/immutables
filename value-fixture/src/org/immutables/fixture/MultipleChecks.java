package org.immutables.fixture;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;

@Value.Enclosing
public interface MultipleChecks {
  interface A {
    int a();

    default @Value.Check void checkA() {
      Preconditions.checkState(a() > 0);
    }
  }

  interface B {
    int b();

    default @Value.Check void checkB() {
      Preconditions.checkState(b() > 0);
    }
  }

  @Value.Immutable
  interface C extends A, B {}
}
