package org.immutables.fixture.style;

import org.immutables.value.BeanStyle;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@BeanStyle.Conservative
abstract class ConservativeStyleDetected {
  abstract List<String> getEm();

  abstract int getValue();

  abstract String getString();

  void use() {
    new ConservativeStyleDetectedBuilder()
        .setValue(1)
        .setString("string")
        .addEm("em")
        .build();
  }
}
