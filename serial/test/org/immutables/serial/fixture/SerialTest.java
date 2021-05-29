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

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

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

  @Test
  public void nullableCollections() throws Exception {
    NullableCollections original = ImmutableNullableCollections.builder().build();
    check(original.collection()).isNull();
    check(original.list()).isNull();
    check(original.set()).isNull();
    check(original.map()).isNull();
    byte[] serializedForm;
    try (ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bytesStream)) {
      oos.writeObject(original);
      serializedForm = bytesStream.toByteArray();
    }
    NullableCollections deserialized;
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serializedForm))) {
      deserialized = (ImmutableNullableCollections) ois.readObject();
    }
    check(deserialized.collection()).isNull();
    check(deserialized.list()).isNull();
    check(deserialized.set()).isNull();
    check(deserialized.map()).isNull();
  }

  @Test
  public void emptyNullableCollections() throws Exception {
    NullableCollections original = ImmutableNullableCollections.builder()
        .collection(Collections.emptySet())
        .list(Collections.emptySet())
        .set(Collections.emptySet())
        .map(Collections.emptyMap())
        .build();
    check(original.collection()).isEmpty();
    check(original.list()).isEmpty();
    check(original.set()).isEmpty();
    check(original.map().entrySet()).isEmpty();
    byte[] serializedForm;
    try (ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bytesStream)) {
      oos.writeObject(original);
      serializedForm = bytesStream.toByteArray();
    }
    NullableCollections deserialized;
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serializedForm))) {
      deserialized = (ImmutableNullableCollections) ois.readObject();
    }
    check(deserialized.collection()).isEmpty();
    check(deserialized.list()).isEmpty();
    check(deserialized.set()).isEmpty();
    check(original.map().entrySet()).isEmpty();
  }
}
