/*
   Copyright 2016 Immutables Authors and Contributors

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
package org.immutables.fixture.jdkonly;

import java.util.OptionalLong;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Optional;
import org.junit.Test;
import static org.immutables.check.Checkers.check;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

// tests how unboxing of jdk optionals is implemented
public class JdkOptionalTest {
  @Test
  public void stringify() {
    check(ImmutableHasOptionalToString.builder()
        .into(1)
        .mandatory("m")
        .nullable("n")
        .optional("o")
        .build()).hasToString("HasOptionalToString{into=1, mandatory=m, nullable=n, optional=o}");

    check(ImmutableHasOptionalToString.builder()
        .mandatory("m")
        .build()).hasToString("HasOptionalToString{mandatory=m}");

    check(ImmutableHasOptionalToString.builder()
        .mandatory("m")
        .nullable("n")
        .build()).hasToString("HasOptionalToString{mandatory=m, nullable=n}");
  }

  @Test
  public void equals() {
    ImmutableJdkOptionals o1 = ImmutableJdkOptionals.of()
        .withV2("v2")
        .withI1(1)
        .withD1(1.0);

    ImmutableJdkOptionals o2 = ImmutableJdkOptionals.of(
        Optional.of("v2"),
        OptionalInt.of(1),
        OptionalLong.empty(),
        OptionalDouble.of(1.0));

    check(o1).is(o2);
    check(o1.hashCode()).is(o2.hashCode());
  }

  @Test
  public void serializeStructural() throws Exception {
    ImmutableJdkOptionals o1 = ImmutableJdkOptionals.of()
        .withV2("v2")
        .withI1(1);

    check(deserialize(serialize(o1))).is(o1);
  }

  @Test
  public void serializeStructuralSingleton() throws Exception {
    ImmutableJdkOptionals o1 = ImmutableJdkOptionals.of();
    check(deserialize(serialize(o1))).same(o1);
  }

  @Test
  public void serializeRegular() throws Exception {
    ImmutableJdkOptionalsSer o0 = ImmutableJdkOptionalsSer.builder()
        .d1(1.0)
        .l1(2L)
        .v2("e4")
        .build();

    Serializable o1 = deserialize(serialize(o0));
    check(o1).is(o0);
    check(o1.hashCode()).is(o0.hashCode());
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
