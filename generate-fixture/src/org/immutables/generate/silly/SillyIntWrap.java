package org.immutables.generate.silly;

import org.immutables.annotation.GenerateConstructorArgument;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaler;

@GenerateImmutable(builder = false)
@GenerateMarshaler
public abstract class SillyIntWrap {

  @GenerateConstructorArgument
  public abstract int value();
}