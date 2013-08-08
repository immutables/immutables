package org.immutables.generate.silly;

import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaler;

@GenerateImmutable
@GenerateMarshaler
public abstract class SillySub1 extends SillyAbstract {
  public abstract int a();
}
