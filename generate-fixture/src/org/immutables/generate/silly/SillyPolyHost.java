package org.immutables.generate.silly;

import java.util.List;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaledAs;
import org.immutables.annotation.GenerateMarshaler;

@GenerateImmutable
@GenerateMarshaler
public abstract class SillyPolyHost {

  @GenerateMarshaledAs(expectedSubclasses = {
      SillySub1.class,
      SillySub2.class
  })
  public abstract List<SillyAbstract> s();
}
