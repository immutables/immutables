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
package org.immutables.serial.fixture;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class StructTest {
  @Test
  public void structFromBuilderSerializeDeserialize() throws Exception {
    ImmutableStructFromBuilder structFromBuilder = ImmutableStructFromBuilder.builder()
        .a(1)
        .os(true)
        .array(true, false)
        .addBag("a")
        .addC("c", "d")
        .s("z")
        .build();

    check(deserialize(serialize(structFromBuilder))).is(structFromBuilder);
  }

  @Test
  public void structFromConstructorSerializeDeserialize() throws Exception {
    ImmutableStructFromConstructor structFromConstructor =
        ImmutableStructFromConstructor.of(
            2,
            Arrays.asList(1, 2),
            Optional.of("OPT"),
            ImmutableMultiset.of("list", "list"),
            ImmutableMultimap.of(1, "1"),
            ImmutableListMultimap.of(2, "2"),
            ImmutableBiMap.of(1, "2"))
            .withIndexSet(ImmutableListMultimap.of(3, "3"));

    check(deserialize(serialize(structFromConstructor))).is(structFromConstructor);
  }

  private Serializable deserialize(byte[] bytes) throws Exception {
    ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
    ObjectInputStream objectStream = new ObjectInputStream(stream);
    return (Serializable) objectStream.readObject();
  }

  private byte[] serialize(Serializable instance) throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    ObjectOutputStream objectStream = new ObjectOutputStream(stream);
    objectStream.writeObject(instance);
    objectStream.close();
    return stream.toByteArray();
  }
}
