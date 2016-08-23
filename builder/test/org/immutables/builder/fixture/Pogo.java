package org.immutables.builder.fixture;

import java.lang.annotation.RetentionPolicy;
import org.immutables.builder.Builder;

public class Pogo {
  final int a;
  final String b;
  final RetentionPolicy policy;

  @Builder.Constructor
  public Pogo(@Builder.Parameter int a, String b, @Builder.Switch RetentionPolicy policy) {
    this.a = a;
    this.b = b;
    this.policy = policy;
  }

  public static void main(String... args) {
    Pogo pogo = new PogoBuilder(1)
        .b("a")
        .runtimePolicy()
        .build();

    pogo.toString();
  }
}
