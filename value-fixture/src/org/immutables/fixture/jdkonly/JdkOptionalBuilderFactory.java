package org.immutables.fixture.jdkonly;

import java.util.OptionalInt;
import com.google.common.base.Optional;
import org.immutables.builder.Builder;

// Compilation test for usage and mixture of optionals for factory builder
public interface JdkOptionalBuilderFactory {
  @Builder.Factory
  public static int appl(Optional<Integer> a, java.util.Optional<String> b, java.util.OptionalInt c) {
    return a.hashCode() + b.hashCode() + c.hashCode();
  }

  @Builder.Factory
  public static int bbz(@Builder.Parameter java.util.Optional<String> b, @Builder.Parameter java.util.OptionalInt c) {
    return b.hashCode() + c.hashCode();
  }

  static void use() {
    new ApplBuilder()
        .a(1)
        .b("")
        .c(1)
        .build();

    new BbzBuilder(java.util.Optional.empty(), OptionalInt.empty()).build();
  }
}
