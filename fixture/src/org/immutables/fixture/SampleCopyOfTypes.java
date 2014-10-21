package org.immutables.fixture;

import java.util.List;
import org.immutables.value.Value;

@Value.Nested
public class SampleCopyOfTypes {
  @Value.Immutable(builder = false)
  public interface ByConstructorAndWithers {
    @Value.Parameter
    int value();

    List<String> additional();
  }

  @Value.Immutable
  public interface ByBuilder {
    int value();
  }
}
