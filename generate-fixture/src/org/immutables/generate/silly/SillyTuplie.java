package org.immutables.generate.silly;

import org.immutables.annotation.GenerateConstructorArgument;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaler;
import com.google.common.base.Optional;
import java.util.Set;

@GenerateImmutable(useBuilder = false)
@GenerateMarshaler
public abstract class SillyTuplie {

  @GenerateConstructorArgument(order = 0)
  public abstract float float1();

  @GenerateConstructorArgument(order = 1)
  public abstract Optional<Byte> opt2();

  @GenerateConstructorArgument(order = 3)
  public abstract Set<Boolean> set3();

}
