/*
   Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.fixture.serial;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class SerialTest {
  @Test
  public void readResolveInterned() throws Exception {
    ImmutableSomeSer instance0 = ImmutableSomeSer.builder().build();
    ImmutableSomeSer instance1 = ImmutableSomeSer.builder().regular(1).build();
    ImmutableSomeSer instance1_2 = ImmutableSomeSer.builder().regular(1).build();

    // interning
    check(deserialize(serialize(instance0))).same(ImmutableSomeSer.builder().build());
    check(deserialize(serialize(instance1))).is(instance1_2);
    check(deserialize(serialize(instance1))).same(instance1_2);

    // singleton
    check(deserialize(serialize(ImmutableOthSer.of()))).same(ImmutableOthSer.builder().build());
  }

  @Test
  public void copySerialVersion() throws Exception {
    for (Field field : ImmutableSomeSer.class.getDeclaredFields()) {
      field.setAccessible(true);
      if (field.getName().equals("serialVersionUID") && field.get(null).equals(1L)) {
        return;
      }
    }

    check(false);
  }

  @Test
  public void serializeModifiable() throws Exception {
    ModifiableSomeSer instance = ModifiableSomeSer.create().setRegular(1);
    // interning
    check(deserialize(serialize(instance))).is(ModifiableSomeSer.create().setRegular(1));
  }

  @Test
  public void copySerialVersionModifiable() throws Exception {
    for (Field field : ModifiableSomeSer.class.getDeclaredFields()) {
      field.setAccessible(true);
      if (field.getName().equals("serialVersionUID") && field.get(null).equals(1L)) {
        return;
      }
    }

    check(false);
  }

  private static Serializable deserialize(byte[] bytes) throws Exception {
    ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
    ObjectInputStream objectStream = new ObjectInputStream(stream);
    return (Serializable) objectStream.readObject();
  }

  private static byte[] serialize(Serializable instance) throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    ObjectOutputStream objectStream = new ObjectOutputStream(stream);
    objectStream.writeObject(instance);
    objectStream.close();
    return stream.toByteArray();
  }
}
