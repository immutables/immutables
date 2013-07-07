package org.immutables.generate.silly;

enum SillyValue {
  ONE, TWO;
  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
