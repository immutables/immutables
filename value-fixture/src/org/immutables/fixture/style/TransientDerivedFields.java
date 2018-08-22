package org.immutables.fixture.style;

import java.io.Serializable;
import org.immutables.serial.Serial;
import org.immutables.value.Value;

@Value.Style // resetting package's style to empty
interface TransientDerivedFields {
  interface Def {
    @Value.Derived
    default int def() {
      return 1;
    }
  }

  @Value.Immutable
  interface Tr extends Def {}

  @Value.Style(transientDerivedFields = false)
  @Value.Immutable
  interface NonTr extends Def {}

  @Value.Immutable
  interface Ser extends Def, Serializable {}

  @Value.Immutable
  @Serial.Structural
  interface StrSer extends Def, Serializable {}
}
