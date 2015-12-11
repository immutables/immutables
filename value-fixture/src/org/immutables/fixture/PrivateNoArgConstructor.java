package org.immutables.fixture;

import org.immutables.value.Value;
import org.immutables.value.Value.Style;

@Value.Immutable
@Style(privateNoargConstructor = true)
interface PrivateNoargConstructorNominal {
  boolean is();
  byte b();
  short s();
  int i();
  long l();
  char c();
  float f();
  double d();
  Object o();
}

@Value.Immutable(prehash=true)
@Style(privateNoargConstructor = true)
interface PrivateNoargConstructorOverridePrehash {

  boolean is();
  byte b();
  short s();
  int i();
  long l();
  char c();
  float f();
  double d();
  Object o();
}

@Value.Immutable(prehash=true)
@Style(privateNoargConstructor = false)
interface PrivateNoargConstructorOptionFalseDoNotAffectPrehash {

  boolean is();
  byte b();
  short s();
  int i();
  long l();
  char c();
  float f();
  double d();
  Object o();
}


@Value.Immutable(singleton = true)
@Style(privateNoargConstructor = true)
interface PrivateNoargConstructorIsOverriddenBySingleton {
  
  @Value.Default
  default int test() {
    return 1;
  }
  
  default void use() {
    ImmutablePrivateNoargConstructorIsOverriddenBySingleton.of();
  }
}
