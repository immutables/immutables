package org.immutables.generate.silly;

import org.immutables.annotation.GenerateConstructorArgument;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaledAs;
import org.immutables.annotation.GenerateMarshaler;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.Set;

@GenerateImmutable
@GenerateMarshaler(importRoutines = SillyMarshalingRoutines.class)
public abstract class SillyMapHolder {

  @GenerateConstructorArgument(order = 0)
  @GenerateMarshaledAs(forceEmpty = true)
  public abstract Map<SillyValue, Integer> holder1();

  @GenerateConstructorArgument(order = 1)
  public abstract Map<Integer, String> holder2();

  public abstract Map<String, SillyMapTup> holder3();

  public abstract Set<RetentionPolicy> zz();
}
