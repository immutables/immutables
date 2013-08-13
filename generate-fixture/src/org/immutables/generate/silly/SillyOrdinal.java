package org.immutables.generate.silly;

import org.immutables.annotation.GenerateConstructorArgument;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.common.collect.OrdinalValue;

@GenerateImmutable(builder = false)
public abstract class SillyOrdinal implements OrdinalValue<SillyOrdinal> {

  @GenerateConstructorArgument
  public abstract String name();
}
