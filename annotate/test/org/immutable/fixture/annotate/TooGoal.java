package org.immutable.fixture.annotate;

import org.immutables.value.Value;

@InjectToo
@Value.Immutable
@Value.Style(allParameters = true, of = "new")
interface TooGoal {
  int a();
  int b();
}
