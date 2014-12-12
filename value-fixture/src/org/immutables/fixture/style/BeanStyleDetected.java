package org.immutables.fixture.style;

import org.immutables.value.Style;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable(copy = true)
@Style.BeanAccessors
abstract class BeanStyleDetected {
  abstract int isIt();

  abstract List<String> getEm();

  void use() {
    ImmutableBeanStyleDetected.builder()
        .it(1)
        .addEm("1")
        .build()
        .withIt(2);
  }
}
