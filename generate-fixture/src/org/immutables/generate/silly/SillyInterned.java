package org.immutables.generate.silly;

import org.immutables.annotation.GenerateConstructorArgument;
import org.immutables.annotation.GenerateImmutable;

@GenerateImmutable(interned = true)
public abstract class SillyInterned {
  @GenerateConstructorArgument(order = 0)
  public abstract int arg1();

  @GenerateConstructorArgument(order = 1)
  public abstract int arg2();
}
