package org.immutables.serial.fixture;

import static org.immutables.check.Checkers.*;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.base.Optional;
import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.junit.Test;

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
