package org.immutables.func.fixture;

import org.immutables.value.Value;
import org.immutables.func.Functional;

@Value.Immutable
public abstract class Compute {
  @Functional
  public abstract String getX();

  @Functional.BindParameters
  public String computeZ(String y) {
    return getX() + y;
  }

  @Functional.BindParameters
  public int computeH(int a, int b, float c) {
    return (int) (a + b + c);
  }
}
