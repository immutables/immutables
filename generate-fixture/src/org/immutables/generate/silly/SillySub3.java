package org.immutables.generate.silly;

import java.util.List;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaler;

@GenerateImmutable
@GenerateMarshaler
public abstract class SillySub3 extends SillyAbstract {
  public abstract List<Double> b();
}
