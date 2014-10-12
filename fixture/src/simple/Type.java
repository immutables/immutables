package simple;

import org.immutables.value.Value;

@Value.Immutable
public class Type {
  @Value.Immutable
  interface Nested {
    @Value.Immutable
    static class Deeper {
      @Value.Immutable
      interface Deepest {}
    }
  }
}
