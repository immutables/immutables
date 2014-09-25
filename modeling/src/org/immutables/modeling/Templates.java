package org.immutables.modeling;

public final class Templates {
  private Templates() {}

  interface Invokation {
    Object param(int ordinal);

    Invokation out(Object content);

    Invokation ln();

    Invokation pos(int line, int column);
  }

  interface Invoker {

  }

  public static Invoker templateName() {
    return null;
  }
  
//  public static Invoker apply(Invoker, ) {
//
//  }
//
//  public static void main(String... args) {
//    templateName()
//      .params(one, two, thre)
//        .body();
//  }
}
