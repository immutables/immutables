package org.immutables.fixture.style;

import org.immutables.value.BeanStyle;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable(copy = true)
@BeanStyle.Accessors
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
