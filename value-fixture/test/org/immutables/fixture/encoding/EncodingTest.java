package org.immutables.fixture.encoding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class EncodingTest {
  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new GuavaModule())
      .registerModule(new Jdk8Module());

  @Test
  public void optMap() throws Exception {
    System.err.println("0");
    UseOptionalCollections<Boolean> value =
        objectMapper.readValue("{\"maybeMap\":{\"a\":true},\"as\":[\"b\"]}",
            new TypeReference<UseOptionalCollections<Boolean>>() {});

    System.out.println("1");
    UseOptionalCollections<Boolean> map = ImmutableUseOptionalCollections.<Boolean>builder()
        .putMaybeMap("a", true)
        .addA("b")
        .build();

    System.err.println("2");
    System.err.println(value);
    System.err.println("3");
    System.err.println(map);

    System.err.println("4");
    check(value).is(map);

    System.err.println("5");
    check(value.maybeMap()).is(Optional.of(ImmutableMap.of("a", true)));
    System.err.println("6");
    check(value.as()).is(Optional.of(ImmutableList.of("b")));
    System.err.println("7");
    check(value.bs()).is(Optional.empty());

  }
}
