package simple;

import org.immutables.annotation.GenerateImmutable;

@GenerateImmutable
public class Type {
  @GenerateImmutable
  interface Nested {
    @GenerateImmutable
    static class Deeper {
      @GenerateImmutable
      interface Deepest {}
    }
  }
}
