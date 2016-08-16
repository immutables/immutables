package org.immutables.value.processor.encode;

public enum StandardNaming {
  NONE("*"),
  GET("*"),
  INIT("*"),
  WITH("with*"),
  ADD("add*", true),
  ADD_ALL("addAll*"),
  PUT("put*", true),
  PUT_ALL("putAll*"),
  IS_SET("*IsSet"),
  SET("set*"),
  UNSET("unset*");

  public final String pattern;
  public final boolean depluralize;

  StandardNaming(String pattern) {
    this(pattern, false);
  }

  StandardNaming(String pattern, boolean depluralize) {
    this.pattern = pattern;
    this.depluralize = depluralize;
  }
}
