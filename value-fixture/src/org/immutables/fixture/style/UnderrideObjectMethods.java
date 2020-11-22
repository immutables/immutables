package org.immutables.fixture.style;

import org.immutables.value.Value;

@Value.Style(underrideEquals = "equalTo", underrideHashCode = "hash", underrideToString = "stringify")
@Value.Immutable
public interface UnderrideObjectMethods {
  int a();

  default String stringify() {
    return "%%%";
  }
  default int hash() {
    return -1;
  }
  default boolean equalTo(UnderrideObjectMethods b) {
    return this == b;
  }

  @Value.Style(underrideEquals = "equalTo", underrideHashCode = "hash", underrideToString = "stringify")
  @Value.Immutable
  interface StaticUnderride {
    static String stringify(StaticUnderride a) {
      return "!!!";
    }
    static int hash(StaticUnderride a) {
      return -2;
    }
    static boolean equalTo(StaticUnderride a, StaticUnderride b) {
      return a == b;
    }
  }

  @Value.Style(underrideEquals = "equalTo")
  @Value.Immutable(intern = true)
  interface InternUnderride {
    int d();
    static boolean equalTo(InternUnderride a, InternUnderride b) {
      return a == b;
    }
  }
}
