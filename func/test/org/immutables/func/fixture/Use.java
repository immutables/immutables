package org.immutables.func.fixture;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

public class Use {
  public static void main(String... args) {
    Predicate<Val> empty = ValFunctions.isEmpty();
    Function<Val, String> name = ValFunctions.getName();
    Function<Val, Integer> age = ValFunctions.age();
    System.out.println(empty);
    System.out.println(name);
    System.out.println(age);
  }
}
