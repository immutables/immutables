package org.immutables.fixture.nested;

import org.immutables.value.Value;

interface BaseFrom {
  String getB();

  boolean isA();
}

@Value.Immutable
interface AbstractSub extends BaseFrom {
  String getC();
}
