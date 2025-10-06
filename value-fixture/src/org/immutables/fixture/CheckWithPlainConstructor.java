package org.immutables.fixture;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(of = "new", allMandatoryParameters = true)
public interface CheckWithPlainConstructor {
  int a();

  default @Value.Default String b() {
    return "whatever";
  }

  default @Value.Check void check() {
    if (a() == 0) throw new IllegalStateException("a == 0 when b is " + b());
  }
}
