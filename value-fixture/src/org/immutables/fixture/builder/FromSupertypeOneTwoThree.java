package org.immutables.fixture.builder;

import org.immutables.value.Value;

interface One {
  String one();
}

interface Two {
  String two();
}

interface Three {
  String three();
}

interface OneAndTwo extends One, Two {}

interface OneAndTwoAndThree extends OneAndTwo, Three {}

@Value.Immutable
interface OneAndTwoConcrete extends OneAndTwo {}

@Value.Immutable
interface OneAndTwoAndThreeConcrete extends OneAndTwoAndThree {}

@SuppressWarnings("all")
public interface FromSupertypeOneTwoThree {
  static void use() {
    One one = null;
    Two two = null;
    Three three = null;
    OneAndTwo oneAndTwo = null;
    OneAndTwoAndThree oneAndTwoAndThree = null;

    System.out.println(ImmutableOneAndTwoConcrete.builder()
        .from(one)
        .from(two)
        .from(oneAndTwo)
        .build());

    System.out.println(ImmutableOneAndTwoAndThreeConcrete.builder()
        .from(one)
        .from(two)
        .from(three)
        .from(oneAndTwo)
        .from(oneAndTwoAndThree)
        .build());
  }
}
