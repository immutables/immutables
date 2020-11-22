package org.immutables.fixture.style;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(limitStringLengthInToString = 2)
public interface TrimToString {
  boolean a();
  String b();
}
