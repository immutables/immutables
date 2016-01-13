package org.immutables.func.fixture;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

public class Use {
  public static void main(String... args) {
    Predicate<ImmutableVal> empty = ValFunctions.isEmpty();
    Function<ImmutableVal, String> name = ValFunctions.getName();
    Function<ImmutableVal, Integer> age = ValFunctions.age();
    System.out.println(empty);
    System.out.println(name);
    System.out.println(age);
  }
}
