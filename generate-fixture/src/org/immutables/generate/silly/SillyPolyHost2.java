package org.immutables.generate.silly;

import com.google.common.base.Optional;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaledAs;
import org.immutables.annotation.GenerateMarshaler;

@GenerateImmutable
@GenerateMarshaler
public abstract class SillyPolyHost2 {

  @GenerateMarshaledAs(expectedSubclasses = {
      SillySub2.class,
      SillySub3.class
  })
  public abstract SillyAbstract s();

  @GenerateMarshaledAs(expectedSubclasses = {
      SillySub2.class,
      SillySub3.class
  })
  public abstract Optional<SillyAbstract> o();
}
