package org.immutables.builder.fixture;

import org.immutables.builder.Builder;
import org.immutables.value.Value;

@Value.Style(newBuilder = "create")
class StyledName {
  final int result;
  @Builder.Constructor
  StyledName(int a, int b) {
    this.result = a + b;
  }

  void use() {
    StyledNameBuilder.create()
        .a(1)
        .b(2)
        .build();
  }
}
