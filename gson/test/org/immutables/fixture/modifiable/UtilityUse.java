package org.immutables.fixture.modifiable;

import org.immutables.value.Value;

@Value.Style(jdkOnly = true)
@Value.Modifiable
public interface UtilityUse {
  int a();

  String b();
}
