package org.immutable.fixture.annotate;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(allParameters = true)
@InjectToo
interface TargetToo {
  String name();

}
