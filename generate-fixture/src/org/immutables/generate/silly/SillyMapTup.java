package org.immutables.generate.silly;

import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import org.immutables.annotation.GenerateConstructorArgument;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaler;

@GenerateImmutable(useBuilder = false)
@GenerateMarshaler
public abstract class SillyMapTup {

  @GenerateConstructorArgument(order = 0)
  public abstract Map<RetentionPolicy, Integer> holder1();

  @GenerateConstructorArgument(order = 1)
  public abstract int value();
}
