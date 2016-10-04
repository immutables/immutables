package org.immutables.builder.fixture.telescopic;

public class Use {
  public static void main(String... args) {
    ImmutableMoji.builder()
        .a(1)
        .b("")
        .build();

    ImmutableStg.builder()
        .a(1)
        .b(1.2)
        .c("c")
        .build();
  }
}
