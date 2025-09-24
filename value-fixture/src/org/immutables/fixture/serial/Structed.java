package org.immutables.fixture.serial;

import org.immutables.value.Value;

@StructStyle
@Value.Immutable
public interface Structed {
  int a();
  String b();
}
