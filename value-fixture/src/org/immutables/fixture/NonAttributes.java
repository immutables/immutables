package org.immutables.fixture;

import org.immutables.value.Value;

// just manually inspected compilation test
public interface NonAttributes {
  int a();
  @Value.NonAttribute
  String b();

  @Value.Immutable
  interface Abc extends NonAttributes {
    // compiler forces to implement abstract non-attribute b()
    @Override
    default String b() {
      return "";
    }
  }
}
