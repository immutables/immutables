package org.immutables.fixture.generatorext;

import org.immutables.value.Value;

@Value.Immutable(builder = false)
public interface SampleRewritedImports {
  @Value.Parameter
  String a();
}
