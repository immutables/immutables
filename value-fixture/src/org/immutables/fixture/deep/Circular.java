package org.immutables.fixture.deep;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(deepImmutablesDetection = true)
@Value.Enclosing
public interface Circular {

  @Value.Immutable
  public interface A {
    B getB();
  }

  @Value.Immutable
  public interface B {
    A getA();
  }
}
