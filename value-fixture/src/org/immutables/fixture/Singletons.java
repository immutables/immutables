package org.immutables.fixture;

import java.util.Collections;
import java.util.List;
import org.immutables.value.Value;

//Compilation tests for singletons/builders
public interface Singletons {}

@Value.Immutable(singleton = true)
interface Sing1 {
  List<Integer> list();

  default void use() {
    ImmutableSing1.builder();
    ImmutableSing1.of()
        .withList(Collections.emptyList());
  }
}

@Value.Immutable(builder = false)
interface Sing2 {
  @Value.Default
  default int a() {
    return 1;
  }

  default void use() {
    ImmutableSing2.of().withA(1);
  }
}

@Value.Immutable(builder = false)
interface Sing3 {
  default void use() {
    ImmutableSing3.of();
  }
}

@Value.Immutable(singleton = true)
interface Sing4 {

  default void use() {
    ImmutableSing4.builder();
    ImmutableSing4.of();
  }
}

@Value.Immutable(builder = false, singleton = true)
interface Sing5 {
  @Value.Parameter
  @Value.Default
  default int a() {
    return 1;
  }

  default void use() {
    ImmutableSing5.of();
    ImmutableSing5.of(1);
  }
}

@Value.Immutable
interface Sing6 {
  default void use() {
    ImmutableSing6.builder();
  }
}

@Value.Immutable(builder = false)
@Value.Style(attributelessSingleton = true)
interface Sing7 {
  default void use() {
    ImmutableSing7.of();
  }
}
