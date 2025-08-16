package org.immutables.fixture.custann;

import nonimmutables.MyVal;

@MyVal
public interface CustomizedUsingArg {
  int a();
  String b();

  @SuppressWarnings("CheckReturnValue")
  static void use() {
    MyValCustomizedUsingArg.builder().a(1).b("A").build();
  }
}
