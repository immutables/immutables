/*
   Copyright 2015-2018 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.serial.fixture;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class SerialTest {

  @Test
  public void copySerialVersion() throws Exception {
    for (Field field : ImmutableVeryLongVeryLongVeryLongVeryLongVeryLongVeryLongVeryLongVeryLongNamedInterface.class
        .getDeclaredFields()) {
      field.setAccessible(true);
      if (field.getName().equals("serialVersionUID") && field.get(null).equals(2L)) {
        return;
      }
    }
    check(false);
  }

  @Test(expected = NoSuchFieldException.class)
  public void serializablePrehashDisabled() throws Exception {
    ImmutableAutodisabled.class.getDeclaredField("hashCode");
  }

  @Test
  public void serializablePrehashReadResolve() throws Exception {
    Method readResolve = ImmutableEnabledWithReadResolve.class.getDeclaredMethod("readResolve");
    check(readResolve.getModifiers() & Modifier.PRIVATE).is(Modifier.PRIVATE);
  }
}
