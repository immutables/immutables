package org.immutables.builder.fixture.telescopic;

import org.immutables.value.Value;

@Value.Style(stagedBuilder = true)
@Value.Immutable
public interface Stg {
  int a();

  double b();

  String c();
}
