package org.immutables.serial.fixture;

import java.lang.reflect.Field;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class SerialTest {

  @Test
  public void copySerialVersion() throws Exception {
    for (Field field : ImmutableVeryLongVeryLongVeryLongVeryLongVeryLongVeryLongVeryLongVeryLongNamedInterface.class.getDeclaredFields()) {
      field.setAccessible(true);
      if (field.getName().equals("serialVersionUID") && field.get(null).equals(2L)) {
        return;
      }
    }
    check(false);
  }
}
