package org.immutables.fixture.serial;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import org.immutables.serial.Serial;
import org.immutables.value.Value;

@JsonSerialize
@Value.Style(depluralize = true)
@Value.Immutable
@Serial.Structural
public abstract class ClazzSerializable implements Serializable {
  private static final long serialVersionUID = -8909894915412895697L;

  public abstract Object obj();
}
