package org.immutables.generate.silly;

import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import javax.annotation.Nullable;

@SuppressWarnings("unused")
public class SillyMarshalingRoutines {

  public static SillyValue unmarshal(
      JsonParser parser,
      @Nullable SillyValue enumNull,
      Class<SillyValue> expectedClass) throws IOException {
    String text = parser.getText();
    return SillyValue.valueOf(text.toUpperCase());
  }
}
